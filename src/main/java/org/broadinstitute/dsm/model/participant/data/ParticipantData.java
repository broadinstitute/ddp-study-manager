package org.broadinstitute.dsm.model.participant.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.Data;
import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.search.join.ScoreMode;
import org.broadinstitute.dsm.db.dao.Dao;
import org.broadinstitute.dsm.db.dao.ddp.instance.DDPInstanceDao;
import org.broadinstitute.dsm.db.dao.ddp.participant.ParticipantDataDao;
import org.broadinstitute.dsm.db.dto.ddp.instance.DDPInstanceDto;
import org.broadinstitute.dsm.db.structure.ColumnName;
import org.broadinstitute.dsm.db.structure.TableName;
import org.broadinstitute.dsm.model.elastic.ESProfile;
import org.broadinstitute.dsm.model.elastic.export.Exportable;
import org.broadinstitute.dsm.model.elastic.export.generate.Generator;
import org.broadinstitute.dsm.model.elastic.export.painless.*;
import org.broadinstitute.dsm.statics.DBConstants;
import org.broadinstitute.dsm.statics.ESObjectConstants;
import org.broadinstitute.dsm.util.proxy.jackson.ObjectMapperSingleton;
import org.broadinstitute.dsm.util.ParticipantUtil;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.NestedQueryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Data
@TableName(
        name = DBConstants.DDP_PARTICIPANT_DATA,
        alias = DBConstants.DDP_PARTICIPANT_DATA_ALIAS,
        primaryKey = DBConstants.PARTICIPANT_DATA_ID,
        columnPrefix = "")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ParticipantData {

    private static final Logger logger = LoggerFactory.getLogger(ParticipantData.class);

    public static final String FIELD_TYPE_PARTICIPANTS = "_PARTICIPANTS";
    public static final Gson GSON = new Gson();

    @ColumnName(DBConstants.PARTICIPANT_DATA_ID)
    private long participantDataId;

    @ColumnName(DBConstants.DDP_PARTICIPANT_ID)
    private String ddpParticipantId;

    private int ddpInstanceId;

    @ColumnName(DBConstants.FIELD_TYPE_ID)
    private String fieldTypeId;

    @ColumnName (DBConstants.DATA)
    private String data;

    @JsonProperty("dynamicFields")
    public Map<String, Object> getDynamicFields() {
        try {
            return ObjectMapperSingleton.readValue(data, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return Map.of();
        }
    }

    public void setData(Map<String, String> data) {
        this.data = ObjectMapperSingleton.writeValueAsString(data);
    }

    public Map<String, String> getData() {
        return ObjectMapperSingleton.readValue(data, new TypeReference<Map<String, String>>() {});
    }

    private Dao dataAccess;

    public ParticipantData() {}

    public ParticipantData(Dao dao) {
        dataAccess = dao;
    }

    public ParticipantData(long participantDataId, String ddpParticipantId, int ddpInstanceId, String fieldTypeId,
                           Map<String, String> data) {
        this.participantDataId = participantDataId;
        this.ddpParticipantId = ddpParticipantId;
        this.ddpInstanceId = ddpInstanceId;
        this.fieldTypeId = fieldTypeId;
        setData(data);
    }

    public static ParticipantData parseDto(@NonNull org.broadinstitute.dsm.db.dto.ddp.participant.ParticipantData participantData) {
        return new ParticipantData(
                participantData.getParticipantDataId(),
                participantData.getDdpParticipantId().orElse(StringUtils.EMPTY),
                participantData.getDdpInstanceId(),
                participantData.getFieldTypeId().orElse(StringUtils.EMPTY),
                GSON.fromJson(participantData.getData().orElse(StringUtils.EMPTY), new TypeToken<Map<String, String>>() {}.getType())
        );
    }

    public static List<ParticipantData> parseDtoList(@NonNull List<org.broadinstitute.dsm.db.dto.ddp.participant.ParticipantData> participantDataList) {
        List<ParticipantData> participantData = new ArrayList<>();
        participantDataList.forEach(dto -> participantData.add(new ParticipantData(
                dto.getParticipantDataId(),
                dto.getDdpParticipantId().orElse(StringUtils.EMPTY),
                dto.getDdpInstanceId(),
                dto.getFieldTypeId().orElse(StringUtils.EMPTY),
                GSON.fromJson(dto.getData().orElse(StringUtils.EMPTY), new TypeToken<Map<String, String>>() {}.getType())
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
            Map<String, String> data = this.getData();
            data.putIfAbsent(column, option);
            setData(data);
        });
    }

    public void setData(String ddpParticipantId, int ddpInstanceId, String fieldTypeId, Map<String, String> data) {
        this.ddpParticipantId = ddpParticipantId;
        this.ddpInstanceId = ddpInstanceId;
        this.fieldTypeId = fieldTypeId;
        setData(data);
    }

    public long insertParticipantData(String userEmail) {
        dataAccess = new ParticipantDataDao();
        org.broadinstitute.dsm.db.dto.ddp.participant.ParticipantData participantData =
                new org.broadinstitute.dsm.db.dto.ddp.participant.ParticipantData.Builder()
                    .withDdpParticipantId(this.ddpParticipantId)
                    .withDdpInstanceId(this.ddpInstanceId)
                    .withFieldTypeId(this.fieldTypeId)
                    .withData(this.data)
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
        participantData.setParticipantDataId(createdDataKey);
        logger.info("Successfully inserted data for participant: " + this.ddpParticipantId);

        DDPInstanceDto ddpInstanceDto = new DDPInstanceDao().getDDPInstanceByInstanceId(ddpInstanceId).orElseThrow();
        String participantGuid = Exportable.getParticipantGuid(ddpParticipantId, ddpInstanceDto.getEsParticipantIndex());

        UpsertPainlessFacade.of(DBConstants.DDP_PARTICIPANT_DATA_ALIAS, participantData, ddpInstanceDto, "participantDataId", "_id", participantGuid)
                .export();

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
                            return StringUtils.EMPTY;
                        })
                        .collect(Collectors.toList());
        return participantRelationshipIds.contains(getRelationshipId());
    }

    String getRelationshipId() {
        return this.getData().getOrDefault(FamilyMemberConstants.RELATIONSHIP_ID, null);
    }

    public boolean updateParticipantData(int participantDataId, String changedByUser) {
        org.broadinstitute.dsm.db.dto.ddp.participant.ParticipantData participantData = new org.broadinstitute.dsm.db.dto.ddp.participant.ParticipantData.Builder()
            .withParticipantDataId(participantDataId)
            .withDdpParticipantId(this.ddpParticipantId)
            .withDdpInstanceId(this.ddpInstanceId)
            .withFieldTypeId(this.fieldTypeId)
            .withData(this.data)
            .withLastChanged(System.currentTimeMillis())
            .withChangedBy(changedByUser)
            .build();

        int rowsAffected = ((ParticipantDataDao) dataAccess).updateParticipantDataColumn(participantData);

        DDPInstanceDto ddpInstanceDto = new DDPInstanceDao().getDDPInstanceByInstanceId(ddpInstanceId).orElseThrow();

        UpsertPainlessFacade.of(DBConstants.DDP_PARTICIPANT_DATA_ALIAS, participantData, ddpInstanceDto, "participantDataId", "participantDataId", participantDataId)
                        .export();

        return rowsAffected == 1;
    }

    private void exportToES(int participantDataId, org.broadinstitute.dsm.db.dto.ddp.participant.ParticipantData participantData, String uniqueIdentifier) {
        DDPInstanceDto ddpInstanceDto = ((DDPInstanceDao) setDataAccess(new DDPInstanceDao())).getDDPInstanceByInstanceId(ddpInstanceId).orElseThrow();

        Generator generator = new ParamsGenerator(participantData, ddpInstanceDto.getInstanceName());
        ScriptBuilder scriptBuilder = new NestedScriptBuilder("participantData", uniqueIdentifier);
        MatchQueryBuilder matchQueryBuilder = new MatchQueryBuilder("participantDataId", participantDataId);
        NestedQueryBuilder nestedQueryBuilder = new NestedQueryBuilder(String.join(".", ESObjectConstants.DSM, generator.getPropertyName()), matchQueryBuilder, ScoreMode.Avg);
        UpsertPainless upsertPainless = new UpsertPainless(generator, ddpInstanceDto.getEsParticipantIndex(), scriptBuilder, nestedQueryBuilder);
        upsertPainless.export();
    }

    public Optional<org.broadinstitute.dsm.db.dto.ddp.participant.ParticipantData> findProband(List<org.broadinstitute.dsm.db.dto.ddp.participant.ParticipantData> participantDataList) {
        return Objects.requireNonNull(participantDataList).stream()
                .filter(participantDataDto -> {
                    Map<String, String> pDataMap = GSON.fromJson(participantDataDto.getData().orElse(StringUtils.EMPTY), Map.class);
                    return FamilyMemberConstants.MEMBER_TYPE_SELF.equals(pDataMap.get(FamilyMemberConstants.MEMBER_TYPE));
                })
                .findFirst();
    }

    public boolean hasFamilyMemberApplicantEmail() {
        if (Objects.isNull(this.data) || StringUtils.isBlank(this.ddpParticipantId)) return false;
        String familyMemberEmail = this.getData().get(FamilyMemberConstants.EMAIL);
        String esParticipantIndex = new DDPInstanceDao().getEsParticipantIndexByInstanceId(ddpInstanceId).orElse(StringUtils.EMPTY);
        String applicantEmail = ParticipantUtil.getParticipantEmailById(esParticipantIndex, this.ddpParticipantId);
        return applicantEmail.equalsIgnoreCase(familyMemberEmail);
    }

    public boolean hasFamilyMemberApplicantEmail(ESProfile applicantProfile) {
        if (Objects.isNull(this.data) || StringUtils.isBlank(this.ddpParticipantId)) return false;
        String familyMemberEmail = this.getData().get(FamilyMemberConstants.EMAIL);
        String applicantEmail = StringUtils.defaultIfBlank(applicantProfile.getEmail(), StringUtils.EMPTY);
        return applicantEmail.equalsIgnoreCase(familyMemberEmail);
    }
    

}
