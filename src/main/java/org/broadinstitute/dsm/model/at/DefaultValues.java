package org.broadinstitute.dsm.model.at;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.dsm.db.DDPInstance;
import org.broadinstitute.dsm.db.dao.Dao;
import org.broadinstitute.dsm.db.dao.bookmark.BookmarkDao;
import org.broadinstitute.dsm.db.dao.ddp.participant.ParticipantDataDao;
import org.broadinstitute.dsm.db.dao.settings.FieldSettingsDao;
import org.broadinstitute.dsm.db.dto.bookmark.BookmarkDto;
import org.broadinstitute.dsm.db.dto.settings.FieldSettingsDto;
import org.broadinstitute.dsm.model.settings.field.FieldSettings;
import org.broadinstitute.dsm.model.participant.data.ParticipantData;
import org.broadinstitute.dsm.statics.ESObjectConstants;
import org.broadinstitute.dsm.util.ElasticSearchUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class DefaultValues {

    private static final Logger logger = LoggerFactory.getLogger(DefaultValues.class);

    private static final String FIELD_TYPE_ID = "AT_GROUP_GENOME_STUDY";
    private static final String GENOME_STUDY_CPT_ID = "GENOME_STUDY_CPT_ID";
    private static final String PREFIX = "DDP_ATCP_";
    private static final String REGISTRATION_TYPE = "REGISTRATION_TYPE";
    public static final String REGISTRATION_TYPE_SELF = "self";
    public static final String REGISTRATION_TYPE_DEPENDENT = "dependent";
    public static final String EXITSTATUS = "EXITSTATUS";

    private static final Gson GSON = new Gson();
    public static final String AT_PARTICIPANT_EXIT = "AT_PARTICIPANT_EXIT";
    public static final String AT_GENOMIC_ID = "at_genomic_id";
    private Dao dataAccess;

    private Map<String, List<org.broadinstitute.dsm.db.ParticipantData>> participantData;
    private Map<String, Map<String, Object>> participantESData;
    private DDPInstance instance;
    private String queryAddition;

    public DefaultValues(Map<String, List<org.broadinstitute.dsm.db.ParticipantData>> participantData,
                         Map<String, Map<String, Object>> participantESData, @NonNull DDPInstance instance, String queryAddition) {
        this.participantData = participantData;
        this.participantESData = participantESData;
        this.instance = instance;
        this.queryAddition = queryAddition;
    }

    public Map<String, List<org.broadinstitute.dsm.db.ParticipantData>> addDefaultValues() {
        if (participantESData == null) {
            logger.warn("Could not update default values, participant ES data is null");
            return participantData;
        }
        boolean addedNewParticipantData = false;
        for (Map.Entry<String, Map<String, Object>> entry: participantESData.entrySet()) {
            String ddpParticipantId = entry.getKey();
            List<org.broadinstitute.dsm.db.ParticipantData> participantDataList = participantData.get(ddpParticipantId);
            if (participantDataList == null) continue;

            if (!hasParticipantDataGenomicId(participantDataList) && isSelfOrDependentParticipant(participantDataList)) {
                Map<String, Object> esParticipantData = entry.getValue();
                Map<String, Object> profile = (Map<String, Object>) esParticipantData.get(ElasticSearchUtil.PROFILE);
                String hruid = (String) profile.get(ESObjectConstants.HRUID);
                addedNewParticipantData = getParticipantGenomicFieldData(participantDataList)
                        .map(pData -> insertGenomicIdIfNotExistsInData(ddpParticipantId, hruid, pData))
                        .orElseGet(() -> insertGenomicIdForParticipant(ddpParticipantId, hruid));
            }

            if (!hasExitedStatusDefaultValue(participantDataList) && isSelfOrDependentParticipant(participantDataList)) {
                addedNewParticipantData = getParticipantExitFieldData(participantDataList)
                        .map(pData -> insertExitStatusIfNotExistsInData(ddpParticipantId, pData))
                        .orElseGet(() -> insertExitStatusForParticipant(ddpParticipantId));
            }

        }
        if (addedNewParticipantData) {
            //participant data was added, getting new list of data
            if (StringUtils.isNotBlank(queryAddition)) {
                participantData = org.broadinstitute.dsm.db.ParticipantData.getParticipantData(instance.getName(), queryAddition);
            }
            else {
                participantData = org.broadinstitute.dsm.db.ParticipantData.getParticipantData(instance.getName());
            }
        }
        return participantData;
    }

    private boolean hasParticipantDataGenomicId(List<org.broadinstitute.dsm.db.ParticipantData> participantDataList) {
        if (participantDataList.isEmpty()) return false;
        return participantDataList.stream()
                .anyMatch(participantData -> {
                    Map<String, String> data = GSON.fromJson(participantData.getData(), Map.class);
                    return data.containsKey(GENOME_STUDY_CPT_ID) && StringUtils.isNotBlank(data.get(GENOME_STUDY_CPT_ID));
                });
    }

    private boolean isSelfOrDependentParticipant(List<org.broadinstitute.dsm.db.ParticipantData> participantDataList) {
        if (participantDataList.isEmpty()) return false;
        return participantDataList.stream()
                .anyMatch(pData -> {
                    Map<String, String> data = GSON.fromJson(pData.getData(), Map.class);
                    return data.containsKey(REGISTRATION_TYPE) &&
                            (REGISTRATION_TYPE_SELF.equalsIgnoreCase(data.get(REGISTRATION_TYPE))
                                    || REGISTRATION_TYPE_DEPENDENT.equalsIgnoreCase(data.get(REGISTRATION_TYPE)));
                });
    }

    private boolean hasExitedStatusDefaultValue(List<org.broadinstitute.dsm.db.ParticipantData> participantDataList) {
        if (participantDataList.isEmpty()) return false;
        return participantDataList.stream()
                .anyMatch(participantData -> {
                    Map<String, String> data = GSON.fromJson(participantData.getData(), new TypeToken<Map<String, String>>() {}.getType());
                    return AT_PARTICIPANT_EXIT.equals(participantData.getFieldTypeId()) && (data.containsKey(EXITSTATUS) && StringUtils.isNotBlank(data.get(EXITSTATUS)));
                });
    }

    private Optional<org.broadinstitute.dsm.db.ParticipantData> getParticipantGenomicFieldData(List<org.broadinstitute.dsm.db.ParticipantData> participantDataList) {
        if (participantDataList.isEmpty()) return Optional.empty();
        return participantDataList.stream()
                .filter(participantData -> FIELD_TYPE_ID.equals(participantData.getFieldTypeId()))
                .findFirst();
    }

    private boolean insertGenomicIdIfNotExistsInData(String ddpParticipantId, String hruid,
                                                     org.broadinstitute.dsm.db.ParticipantData pData) {
        Map<String, String> dataMap = GSON.fromJson(pData.getData(), new TypeToken<Map<String, String>>() {}.getType());
        if (dataMap.containsKey(GENOME_STUDY_CPT_ID)) return false;
        dataMap.put(GENOME_STUDY_CPT_ID, PREFIX.concat(getGenomicIdValue(hruid)));
        return updateParticipantData(ddpParticipantId, pData, dataMap);
    }

    private String getGenomicIdValue(String hruid) {
        this.setDataAccess(new BookmarkDao());
        BookmarkDao dataAccess = (BookmarkDao) this.dataAccess;
        Optional<BookmarkDto> maybeGenomicId = dataAccess.getBookmarkByInstance(AT_GENOMIC_ID);
        return maybeGenomicId
                .map(bookmarkDto -> {
                    dataAccess.updateBookmarkValueByBookmarkId(bookmarkDto.getBookmarkId(), bookmarkDto.getValue() + 1);
                    return String.valueOf(bookmarkDto.getValue());
                })
                .orElse(hruid);
    }

    private boolean insertGenomicIdForParticipant(String ddpParticipantId, String hruid) {
        if (StringUtils.isBlank(hruid)) return false;
        return insertParticipantData(Map.of(GENOME_STUDY_CPT_ID, PREFIX.concat(getGenomicIdValue(hruid))), ddpParticipantId, FIELD_TYPE_ID);
    }

    private Optional<org.broadinstitute.dsm.db.ParticipantData> getParticipantExitFieldData(List<org.broadinstitute.dsm.db.ParticipantData> participantDataList) {
        if (participantDataList.isEmpty()) return Optional.empty();
        return participantDataList.stream()
                .filter(participantData -> AT_PARTICIPANT_EXIT.equals(participantData.getFieldTypeId()))
                .findFirst();
    }

    private boolean insertExitStatusIfNotExistsInData(String ddpParticipantId, org.broadinstitute.dsm.db.ParticipantData pData) {
        Map<String, String> dataMap = GSON.fromJson(pData.getData(), new TypeToken<Map<String, String>>() {}.getType());
        if (dataMap.containsKey(EXITSTATUS)) return false;
        dataMap.put(EXITSTATUS, getDefaultExitStatus());
        return updateParticipantData(ddpParticipantId, pData, dataMap);
    }

    private boolean insertExitStatusForParticipant(String ddpParticipantId) {
        String datstatExitReasonDefaultOption = getDefaultExitStatus();
        return insertParticipantData(Map.of(EXITSTATUS, datstatExitReasonDefaultOption), ddpParticipantId, AT_PARTICIPANT_EXIT);
    }

    private boolean updateParticipantData(String ddpParticipantId, org.broadinstitute.dsm.db.ParticipantData pData, Map<String, String> dataMap) {
        this.setDataAccess(new ParticipantDataDao());
        ParticipantData participantData = new ParticipantData(dataAccess);
        participantData.setData(ddpParticipantId, Integer.parseInt(instance.getDdpInstanceId()), pData.getFieldTypeId(), dataMap);
        return participantData.updateParticipantData(Integer.parseInt(pData.getDataId()), "SYSTEM");
    }

    private boolean insertParticipantData(Map<String, String> data, String ddpParticipantId, String fieldTypeId) {
        this.setDataAccess(new ParticipantDataDao());
        ParticipantData participantData = new ParticipantData(dataAccess);
        participantData.setData(ddpParticipantId, Integer.parseInt(instance.getDdpInstanceId()),
                fieldTypeId, data);
        try {
            participantData.insertParticipantData("SYSTEM");
            logger.info("values: " + data.keySet().stream().collect(Collectors.joining(", ", "[", "]")) + " were created for participant with id: " + ddpParticipantId + " at " + FIELD_TYPE_ID);
            return true;
        } catch (RuntimeException re) {
            return false;
        }
    }

    private String getDefaultExitStatus() {
        this.setDataAccess(FieldSettingsDao.of());
        Optional<FieldSettingsDto> fieldSettingByColumnNameAndInstanceId = ((FieldSettingsDao) dataAccess)
                .getFieldSettingByColumnNameAndInstanceId(Integer.parseInt(instance.getDdpInstanceId()), EXITSTATUS);
        return fieldSettingByColumnNameAndInstanceId.
                map(fieldSettingsDto -> {
                    FieldSettings fieldSettings = new FieldSettings();
                    return fieldSettings.getDefaultOptionValue(fieldSettingsDto.getPossibleValues());
                })
                .orElse("");
    }

    private void setDataAccess(Dao dao) {
        this.dataAccess = dao;
    }
}
