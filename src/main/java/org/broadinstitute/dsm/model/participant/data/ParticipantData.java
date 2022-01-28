package org.broadinstitute.dsm.model.participant.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.Data;
import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.dsm.db.dao.Dao;
import org.broadinstitute.dsm.db.dao.ddp.instance.DDPInstanceDao;
import org.broadinstitute.dsm.db.dao.ddp.participant.ParticipantDataDao;
import org.broadinstitute.dsm.model.elastic.ESProfile;
import org.broadinstitute.dsm.util.proxy.jackson.ObjectMapperSingleton;
import org.broadinstitute.dsm.util.ParticipantUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Data
public class ParticipantData {

    private static final Logger logger = LoggerFactory.getLogger(ParticipantData.class);

    public static final String FIELD_TYPE_PARTICIPANTS = "_PARTICIPANTS";
    public static final Gson GSON = new Gson();

    private long dataId;
    private String ddpParticipantId;
    private int ddpInstanceId;
    private String fieldTypeId;
    private String data;

    public void setData(Map<String, String> data) {
        this.data = ObjectMapperSingleton.writeValueAsString(data);
    }

    public Map<String, String> getData() {
        return ObjectMapperSingleton.readValue(data, Map.class);
    }

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

    public static ParticipantData parseDto(@NonNull org.broadinstitute.dsm.db.dto.ddp.participant.ParticipantData participantData) {
        return new ParticipantData(
                participantData.getParticipantDataId(),
                participantData.getDdpParticipantId().orElse(""),
                participantData.getDdpInstanceId(),
                participantData.getFieldTypeId().orElse(""),
                GSON.fromJson(participantData.getData().orElse(""), new TypeToken<Map<String, String>>() {}.getType())
        );
    }

    public static List<ParticipantData> parseDtoList(@NonNull List<org.broadinstitute.dsm.db.dto.ddp.participant.ParticipantData> participantDataList) {
        List<ParticipantData> participantData = new ArrayList<>();
        participantDataList.forEach(dto -> participantData.add(new ParticipantData(
                dto.getParticipantDataId(),
                dto.getDdpParticipantId().orElse(""),
                dto.getDdpInstanceId(),
                dto.getFieldTypeId().orElse(""),
                GSON.fromJson(dto.getData().orElse(""), new TypeToken<Map<String, String>>() {}.getType())
        )));
        return participantData;
    }

    public List<org.broadinstitute.dsm.db.dto.ddp.participant.ParticipantData> getParticipantDataByParticipantId(String ddpParticipantId) {
        if (StringUtils.isBlank(ddpParticipantId)) return Collections.emptyList();
        ParticipantDataDao dataAccess = (ParticipantDataDao) setDataAccess(new ParticipantDataDao());
        return dataAccess.getParticipantDataByParticipantId(ddpParticipantId);
    }

    private Dao setDataAccess(Dao dao) {
        this.dataAccess = dao;
        return this.dataAccess;
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

    public long insertParticipantData(String userEmail) {
        dataAccess = new ParticipantDataDao();
        org.broadinstitute.dsm.db.dto.ddp.participant.ParticipantData participantData =
                new org.broadinstitute.dsm.db.dto.ddp.participant.ParticipantData.Builder()
                    .withDdpParticipantId(this.ddpParticipantId)
                    .withDdpInstanceId(this.ddpInstanceId)
                    .withFieldTypeId(this.fieldTypeId)
                    .withData(GSON.toJson(this.data))
                    .withLastChanged(System.currentTimeMillis())
                    .withChangedBy(userEmail)
                    .build();
        if (isRelationshipIdExists()) {
            throw new RuntimeException(String.format("Family member with that Relationship ID: %s already exists", getRelationshipId()));
        }
        int createdDataKey = dataAccess.create(participantData);
        if (createdDataKey < 1) {
            throw new RuntimeException("Could not insert participant data for : " + this.ddpParticipantId);
        }
        logger.info("Successfully inserted data for participant: " + this.ddpParticipantId);
        return createdDataKey;
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
        org.broadinstitute.dsm.db.dto.ddp.participant.ParticipantData participantData = new org.broadinstitute.dsm.db.dto.ddp.participant.ParticipantData.Builder()
            .withParticipantDataId(dataId)
            .withDdpParticipantId(this.ddpParticipantId)
            .withDdpInstanceId(this.ddpInstanceId)
            .withFieldTypeId(this.fieldTypeId)
            .withData(GSON.toJson(this.data))
            .withLastChanged(System.currentTimeMillis())
            .withChangedBy(changedByUser)
            .build();
        int rowsAffected = ((ParticipantDataDao) dataAccess).updateParticipantDataColumn(participantData);
        return rowsAffected == 1;
    }

    public Optional<org.broadinstitute.dsm.db.dto.ddp.participant.ParticipantData> findProband(List<org.broadinstitute.dsm.db.dto.ddp.participant.ParticipantData> participantDataList) {
        return Objects.requireNonNull(participantDataList).stream()
                .filter(participantDataDto -> {
                    Map<String, String> pDataMap = GSON.fromJson(participantDataDto.getData().orElse(""), Map.class);
                    return FamilyMemberConstants.MEMBER_TYPE_SELF.equals(pDataMap.get(FamilyMemberConstants.MEMBER_TYPE));
                })
                .findFirst();
    }

    public boolean hasFamilyMemberApplicantEmail() {
        if (Objects.isNull(this.data) || StringUtils.isBlank(this.ddpParticipantId)) return false;
        String familyMemberEmail = this.data.get(FamilyMemberConstants.EMAIL);
        String esParticipantIndex = new DDPInstanceDao().getEsParticipantIndexByInstanceId(ddpInstanceId).orElse("");
        String applicantEmail = ParticipantUtil.getParticipantEmailById(esParticipantIndex, this.ddpParticipantId);
        return applicantEmail.equalsIgnoreCase(familyMemberEmail);
    }

    public boolean hasFamilyMemberApplicantEmail(ESProfile applicantProfile) {
        if (Objects.isNull(this.data) || StringUtils.isBlank(this.ddpParticipantId)) return false;
        String familyMemberEmail = this.data.get(FamilyMemberConstants.EMAIL);
        String applicantEmail = StringUtils.defaultIfBlank(applicantProfile.getEmail(), "");
        return applicantEmail.equalsIgnoreCase(familyMemberEmail);
    }

    public String getFamilyMemberEmail() {
        return Objects.requireNonNull(this.data).get(FamilyMemberConstants.EMAIL);
    }


}
