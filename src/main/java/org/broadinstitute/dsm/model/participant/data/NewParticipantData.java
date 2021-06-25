package org.broadinstitute.dsm.model.participant.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.Data;
import lombok.NonNull;
import org.broadinstitute.dsm.db.dao.Dao;
import org.broadinstitute.dsm.db.dao.ddp.participant.ParticipantDataDao;
import org.broadinstitute.dsm.db.dto.ddp.participant.ParticipantDataDto;
import org.broadinstitute.dsm.db.dao.ddp.instance.DDPInstanceDao;
import org.broadinstitute.dsm.db.dao.participant.data.ParticipantDataDao;
import org.broadinstitute.dsm.db.dto.participant.data.ParticipantDataDto;
import org.broadinstitute.dsm.model.elasticsearch.ESProfile;
import org.broadinstitute.dsm.model.elasticsearch.ElasticSearch;
import org.broadinstitute.dsm.util.ElasticSearchUtil;

@Data
public class NewParticipantData {

    public static final String FIELD_TYPE = "_PARTICIPANTS";

    private long dataId;
    private String ddpParticipantId;
    private int ddpInstanceId;
    private String fieldTypeId;
    private Map<String, String> data;

    private Dao dataAccess;

    public NewParticipantData() {}

    public NewParticipantData(Dao dao) {
        dataAccess = dao;
    }

    public NewParticipantData(long participantDataId, String ddpParticipantId, int ddpInstanceId, String fieldTypeId,
                              Map<String, String> data) {
        this.dataId = participantDataId;
        this.ddpParticipantId = ddpParticipantId;
        this.ddpInstanceId = ddpInstanceId;
        this.fieldTypeId = fieldTypeId;
        this.data = data;
    }

    public static NewParticipantData parseDto(@NonNull ParticipantDataDto participantDataDto) {
        return new NewParticipantData(
                participantDataDto.getParticipantDataId(),
                participantDataDto.getDdpParticipantId(),
                participantDataDto.getDdpInstanceId(),
                participantDataDto.getFieldTypeId(),
                new Gson().fromJson(participantDataDto.getData(), new TypeToken<Map<String, String>>() {}.getType())
        );
    }

    public static List<NewParticipantData> parseDtoList(@NonNull List<ParticipantDataDto> participantDataDtoList) {
        List<NewParticipantData> participantData = new ArrayList<>();
        participantDataDtoList.forEach(dto -> participantData.add(new NewParticipantData(
                dto.getParticipantDataId(),
                dto.getDdpParticipantId(),
                dto.getDdpInstanceId(),
                dto.getFieldTypeId(),
                new Gson().fromJson(dto.getData(), new TypeToken<Map<String, String>>() {}.getType())
        )));
        return participantData;
    }

    public Map<String, String> mergeParticipantData(@NonNull AddFamilyMemberPayload familyMemberPayload) {
        FamilyMemberDetails familyMemberData =
                familyMemberPayload.getData().orElseThrow(() -> new NoSuchElementException("Family member data is not provided"));
        Map<String, String> mergedData = new HashMap<>();
        boolean copyProbandInfo = familyMemberPayload.getCopyProbandInfo().orElse(Boolean.FALSE);
        int probandDataId = familyMemberPayload.getProbandDataId().orElse(0);
        if (copyProbandInfo && probandDataId > 0) {
            Optional<NewParticipantData> maybeParticipantData = dataAccess.get(probandDataId).map(pd -> parseDto((ParticipantDataDto)pd));
            maybeParticipantData.ifPresent(p -> mergedData.putAll(p.getData()));
        } else {
            familyMemberPayload.getParticipantId().ifPresent(pId -> familyMemberData.setEmail(getParticipantEmailById(pId)));
        }
        familyMemberData.toMap().forEach((k, v) -> mergedData.compute(k, (probandKey, probandVal) -> v != null ? v : probandVal));
        return mergedData;
    }

    private String getParticipantEmailById(String pId) {
        dataAccess = new DDPInstanceDao();
        StringBuilder email = new StringBuilder();
        Optional<String> maybeEsParticipantIndex =
                ((DDPInstanceDao) dataAccess).getEsParticipantIndexByInstanceId(ddpInstanceId);
        maybeEsParticipantIndex.ifPresent(esParticipantIndex -> {
            ElasticSearch participantESDataByParticipantId =
                    ElasticSearchUtil.getParticipantESDataByParticipantId(esParticipantIndex, pId);
            email.append(participantESDataByParticipantId.getProfile()
                    .map(ESProfile::getEmail)
                    .orElse(""));
        });
        return email.toString();
    }

    public void addDefaultOptionsValueToData(@NonNull Map<String, String> columnsWithDefaultOptions) {
        columnsWithDefaultOptions.forEach((column, option) -> {
            this.data.putIfAbsent(column, option);
        });
    }

    public void setData(String ddpParticipantId, int ddpInstanceId, String fieldTypeId, Map<String, String> data) {
        this.ddpParticipantId = ddpParticipantId;
        this.ddpInstanceId = ddpInstanceId;
        this.fieldTypeId = fieldTypeId;
        this.data = data;
    }

    public void insertParticipantData(String userEmail) {
        dataAccess = new ParticipantDataDao();
        ParticipantDataDto participantDataDto =
                new ParticipantDataDto(this.ddpParticipantId, this.ddpInstanceId, this.fieldTypeId, new Gson().toJson(this.data),
                        System.currentTimeMillis(), userEmail);
        if (isRelationshipIdExists()) {
            throw new RuntimeException(String.format("Family member with that Relationship ID: %s already exists", getRelationshipId()));
        }
        int createdDataKey = dataAccess.create(participantDataDto);
        if (createdDataKey < 1) {
            throw new RuntimeException("Could not insert participant data for : " + this.ddpParticipantId);
        }
    }

    public boolean isRelationshipIdExists() {
        List<String> participantRelationshipIds =
                parseDtoList(((ParticipantDataDao) dataAccess).getParticipantDataByParticipantId(this.ddpParticipantId)).stream()
                        .map(pData -> {
                            Map<String, String> familyMemberData = pData.getData();
                            boolean hasRelationshipId = familyMemberData.containsKey(FamilyMemberConstants.RELATIONSHIP_ID);
                            if (hasRelationshipId) {
                                return familyMemberData.get(FamilyMemberConstants.RELATIONSHIP_ID);
                            }
                            return "";
                        })
                        .collect(Collectors.toList());
        return participantRelationshipIds.contains(getRelationshipId());
    }

    String getRelationshipId() {
        return this.data.getOrDefault(FamilyMemberConstants.RELATIONSHIP_ID, null);
    }

    public boolean updateParticipantData(int dataId, String changedByUser) {
        ParticipantDataDto participantDataDto =
                new ParticipantDataDto(dataId, this.ddpParticipantId, this.ddpInstanceId, this.fieldTypeId, new Gson().toJson(this.data),
                        System.currentTimeMillis(), changedByUser);
        int rowsAffected = ((ParticipantDataDao) dataAccess).updateParticipantDataColumn(participantDataDto);
        return rowsAffected == 1;
    }
}
