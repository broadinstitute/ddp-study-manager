package org.broadinstitute.dsm.model.participant.data;

import java.util.ArrayList;
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
import org.broadinstitute.dsm.model.elasticsearch.ESProfile;
import org.broadinstitute.dsm.model.elasticsearch.ElasticSearch;
import org.broadinstitute.dsm.util.ElasticSearchUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Data
public class ParticipantData {

    private static final Logger logger = LoggerFactory.getLogger(ParticipantData.class);

    public static final String FIELD_TYPE = "_PARTICIPANTS";

    private long dataId;
    private String ddpParticipantId;
    private int ddpInstanceId;
    private String fieldTypeId;
    private Map<String, String> data;

    private Dao dataAccess;

    public ParticipantData() {}

    public ParticipantData(Dao dao) {
        dataAccess = dao;
    }

    public ParticipantData(long participantDataId, String ddpParticipantId, int ddpInstanceId, String fieldTypeId,
                           Map<String, String> data) {
        this.dataId = participantDataId;
        this.ddpParticipantId = ddpParticipantId;
        this.ddpInstanceId = ddpInstanceId;
        this.fieldTypeId = fieldTypeId;
        this.data = data;
    }

    public static ParticipantData parseDto(@NonNull ParticipantDataDto participantDataDto) {
        return new ParticipantData(
                participantDataDto.getParticipantDataId(),
                participantDataDto.getDdpParticipantId(),
                participantDataDto.getDdpInstanceId(),
                participantDataDto.getFieldTypeId(),
                new Gson().fromJson(participantDataDto.getData(), new TypeToken<Map<String, String>>() {}.getType())
        );
    }

    public static List<ParticipantData> parseDtoList(@NonNull List<ParticipantDataDto> participantDataDtoList) {
        List<ParticipantData> participantData = new ArrayList<>();
        participantDataDtoList.forEach(dto -> participantData.add(new ParticipantData(
                dto.getParticipantDataId(),
                dto.getDdpParticipantId(),
                dto.getDdpInstanceId(),
                dto.getFieldTypeId(),
                new Gson().fromJson(dto.getData(), new TypeToken<Map<String, String>>() {}.getType())
        )));
        return participantData;
    }

    public void setFamilyMemberData(@NonNull AddFamilyMemberPayload familyMemberPayload) {
        FamilyMemberDetails familyMemberData =
                familyMemberPayload.getData().orElseThrow(() -> new NoSuchElementException("Family member data is not provided"));
        DDPInstanceDao dataAccess = (DDPInstanceDao) setDataAccess(new DDPInstanceDao());
        String collaboratorIdPrefix = dataAccess.getCollaboratorIdPrefixByStudyGuid(familyMemberPayload.getRealm().orElseThrow())
                .orElse(familyMemberPayload.getRealm().get());
        familyMemberData.setCollaboratorParticipantId(
                collaboratorIdPrefix +
                "_" +
                familyMemberData.getCollaboratorParticipantId());
        this.data = familyMemberData.toMap();
    }

    public void copyProbandData(AddFamilyMemberPayload familyMemberPayload) {
        boolean isCopyProband = familyMemberPayload.getCopyProbandInfo().orElse(Boolean.FALSE);
        int probandDataId = familyMemberPayload.getProbandDataId().orElse(0);
        if (!isCopyProband || probandDataId == 0) return;
        ParticipantDataDao dataAccess = (ParticipantDataDao) setDataAccess(new ParticipantDataDao());
        Optional<ParticipantData> maybeParticipantData = dataAccess.get(probandDataId).map(ParticipantData::parseDto);
        maybeParticipantData.ifPresent(participantData -> participantData.data.forEach((k, v) -> this.data.putIfAbsent(k, v)));
    }

    private Dao setDataAccess(Dao dao) {
        this.dataAccess = dao;
        return this.dataAccess;
    }

    private String getParticipantEmailById(String pId) {
        dataAccess = new DDPInstanceDao();
        StringBuilder email = new StringBuilder();
        Optional<String> maybeEsParticipantIndex =
                ((DDPInstanceDao) dataAccess).getEsParticipantIndexByInstanceId(ddpInstanceId);
        maybeEsParticipantIndex.ifPresent(esParticipantIndex -> {
            ElasticSearch participantESDataByParticipantId =
                    ElasticSearchUtil.getParticipantESDataByParticipantId(esParticipantIndex, pId)
                    .orElse(new ElasticSearch.Builder().build());
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
        logger.info("Successfully inserted data for participant: " + this.ddpParticipantId);
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
