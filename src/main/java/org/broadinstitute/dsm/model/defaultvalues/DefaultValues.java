package org.broadinstitute.dsm.model.defaultvalues;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.dsm.db.DDPInstance;
import org.broadinstitute.dsm.db.dao.Dao;
import org.broadinstitute.dsm.db.dao.bookmark.BookmarkDao;
import org.broadinstitute.dsm.db.dao.ddp.participant.ParticipantDataDao;
import org.broadinstitute.dsm.db.dao.settings.FieldSettingsDao;
import org.broadinstitute.dsm.db.dto.bookmark.BookmarkDto;
import org.broadinstitute.dsm.db.dto.ddp.participant.ParticipantData;
import org.broadinstitute.dsm.db.dto.settings.FieldSettingsDto;
import org.broadinstitute.dsm.model.ddp.DDPActivityConstants;
import org.broadinstitute.dsm.model.elastic.ESActivities;
import org.broadinstitute.dsm.model.elastic.ESProfile;
import org.broadinstitute.dsm.model.elastic.search.ElasticSearchParticipantDto;
import org.broadinstitute.dsm.model.settings.field.FieldSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DefaultValues extends BasicDefaultDataMaker {

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
    public static final String ACTIVITY_CODE_PREQUAL = "PREQUAL";
    public static final String PREQUAL_SELF_DESCRIBE = "PREQUAL_SELF_DESCRIBE";
    public static final String QUESTION_ANSWER = "answer";
    public static final String SELF_DESCRIBE_CHILD_DIAGNOSED = "CHILD_DIAGNOSED";
    public static final String SELF_DESCRIBE_DIAGNOSED = "DIAGNOSED";
    private Dao dataAccess;

    private Map<String, List<ParticipantData>> participantData;
    private List<ElasticSearchParticipantDto> participantESData;
    private DDPInstance instance;
    private String queryAddition;
    private ParticipantDataDao participantDataDao;

//
//    public DefaultValues(Map<String, List<ParticipantData>> participantData,
//                         List<ElasticSearchParticipantDto> participantESData, @NonNull DDPInstance instance, String queryAddition) {
//        this.participantData = participantData;
//        this.participantESData = participantESData;
//        this.instance = instance;
//        this.queryAddition = queryAddition;
//        this.participantDataDao = new ParticipantDataDao();
//    }

    public Map<String, List<ParticipantData>> addDefaultValues() {

        if (participantESData == null) {
            logger.warn("Could not update default values, participant ES data is null");
            return participantData;
        }
        boolean addedNewParticipantData = false;
        Map<String, ElasticSearchParticipantDto> participantEsDataByParticipantId =
                participantESData.stream().collect(Collectors.toMap(ElasticSearchParticipantDto::getParticipantId, Function.identity()));
        for (Map.Entry<String, ElasticSearchParticipantDto> entry: participantEsDataByParticipantId.entrySet()) {
            String ddpParticipantId = entry.getKey();
            List<ParticipantData> participantDataList = participantData.get(ddpParticipantId);
            if (participantDataList == null) continue;
//
//            if (!hasParticipantDataGenomicId(participantDataList) && isSelfOrDependentParticipant(participantDataList)) {
//                ElasticSearchParticipantDto esParticipantData = entry.getValue();
//                String hruid = esParticipantData.getProfile().map(ESProfile::getHruid).orElse("");
//                addedNewParticipantData = getParticipantGenomicFieldData(participantDataList)
//                        .map(pData -> insertGenomicIdIfNotExistsInData(ddpParticipantId, hruid, pData))
//                        .orElseGet(() -> insertGenomicIdForParticipant(ddpParticipantId, hruid));
//            }
//
//            if (!hasExitedStatusDefaultValue(participantDataList) && isSelfOrDependentParticipant(participantDataList)) {
//                addedNewParticipantData = getParticipantExitFieldData(participantDataList)
//                        .map(pData -> insertExitStatusIfNotExistsInData(ddpParticipantId, pData))
//                        .orElseGet(() -> insertExitStatusForParticipant(ddpParticipantId));
//            }

        }
        if (addedNewParticipantData) {
            //participant data was added, getting new list of data
            if (StringUtils.isNotBlank(queryAddition)) {
                List<ParticipantData> participantDataByInstanceIdAndQueryAddition =
                        participantDataDao.getParticipantDataByInstanceIdAndQueryAddition(Integer.parseInt(instance.getDdpInstanceId()),
                                queryAddition);
                participantData = getParticipantDataWithParticipantId(participantDataByInstanceIdAndQueryAddition);
            }
            else {
                List<ParticipantData> participantDataByInstanceId =
                        participantDataDao.getParticipantDataByInstanceId(Integer.parseInt(instance.getDdpInstanceId()));
                participantData = getParticipantDataWithParticipantId(participantDataByInstanceId);
            }
        }
        return participantData;
    }

    private Map<String, List<ParticipantData>> getParticipantDataWithParticipantId(List<ParticipantData> participantDataByInstanceId) {
        return participantDataByInstanceId.stream()
                .collect(Collectors.toMap(
                        pDataDto -> pDataDto.getDdpParticipantId().orElse(""),
                        pDataDto -> new ArrayList<>(List.of(pDataDto)),
                        (prev, curr) -> {
                            prev.addAll(curr);
                            return prev;
                        }
                ));
    }

    private boolean hasParticipantDataGenomicId(List<ParticipantData> participantDataList) {
        if (participantDataList.isEmpty()) return false;
        return participantDataList.stream()
                .anyMatch(participantData -> {
                    Map<String, String> data = GSON.fromJson(participantData.getData().orElse(""), new TypeToken<Map<String, String>>() {}.getType());
                    return data.containsKey(GENOME_STUDY_CPT_ID) && StringUtils.isNotBlank(data.get(GENOME_STUDY_CPT_ID));
                });
    }

    boolean isSelfOrDependentParticipant() {
        if (elasticSearchParticipantDto.getActivities().isEmpty()) return false;
        return elasticSearchParticipantDto.getActivities().stream()
                .anyMatch(this::isPrequalAndSelfOrDependent);
    }

    private boolean isPrequalAndSelfOrDependent(ESActivities activity) {
        return ACTIVITY_CODE_PREQUAL.equals(activity.getActivityCode()) &&
                (activity.getQuestionsAnswers().stream()
                        .filter(anwers -> PREQUAL_SELF_DESCRIBE.equals(anwers.get(DDPActivityConstants.DDP_ACTIVITY_STABLE_ID)))
                        .anyMatch(answers -> ((List)answers.get(QUESTION_ANSWER)).stream().anyMatch(answer -> SELF_DESCRIBE_CHILD_DIAGNOSED.equals(answer) ||
                                SELF_DESCRIBE_DIAGNOSED.equals(answer))));
    }

    private boolean hasExitedStatusDefaultValue(List<ParticipantData> participantDataList) {
        if (participantDataList.isEmpty()) return false;
        return participantDataList.stream()
                .anyMatch(participantData -> {
                    Map<String, String> data = GSON.fromJson(participantData.getData().orElse(""), new TypeToken<Map<String, String>>() {}.getType());
                    return AT_PARTICIPANT_EXIT.equals(participantData.getFieldTypeId().orElse("")) && (data.containsKey(EXITSTATUS) && StringUtils.isNotBlank(data.get(EXITSTATUS)));
                });
    }

    private Optional<ParticipantData> getParticipantGenomicFieldData(List<ParticipantData> participantDataList) {
        if (participantDataList.isEmpty()) return Optional.empty();
        return participantDataList.stream()
                .filter(participantData -> FIELD_TYPE_ID.equals(participantData.getFieldTypeId().orElse("")))
                .findFirst();
    }

    private boolean insertGenomicIdIfNotExistsInData(String ddpParticipantId, String hruid,
                                                     ParticipantData pData) {
        Map<String, String> dataMap = GSON.fromJson(pData.getData().orElse(""), new TypeToken<Map<String, String>>() {}.getType());
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

    private Optional<ParticipantData> getParticipantExitFieldData(List<ParticipantData> participantDataList) {
        if (participantDataList.isEmpty()) return Optional.empty();
        return participantDataList.stream()
                .filter(participantData -> AT_PARTICIPANT_EXIT.equals(participantData.getFieldTypeId().orElse("")))
                .findFirst();
    }

    private boolean insertExitStatusIfNotExistsInData(String ddpParticipantId, ParticipantData pData) {
        Map<String, String> dataMap = GSON.fromJson(pData.getData().orElse(""), new TypeToken<Map<String, String>>() {}.getType());
        if (dataMap.containsKey(EXITSTATUS)) return false;
        dataMap.put(EXITSTATUS, getDefaultExitStatus());
        return updateParticipantData(ddpParticipantId, pData, dataMap);
    }

    private boolean insertExitStatusForParticipant(String ddpParticipantId) {
        String datstatExitReasonDefaultOption = getDefaultExitStatus();
        return insertParticipantData(Map.of(EXITSTATUS, datstatExitReasonDefaultOption), ddpParticipantId, AT_PARTICIPANT_EXIT);
    }

    private boolean updateParticipantData(String ddpParticipantId, ParticipantData pData, Map<String, String> dataMap) {
        this.setDataAccess(new ParticipantDataDao());
        org.broadinstitute.dsm.model.participant.data.ParticipantData participantData = new org.broadinstitute.dsm.model.participant.data.ParticipantData(dataAccess);
        participantData.setData(ddpParticipantId, Integer.parseInt(instance.getDdpInstanceId()), pData.getFieldTypeId().orElse(""), dataMap);
        return participantData.updateParticipantData(pData.getParticipantDataId(), "SYSTEM");
    }

    private boolean insertParticipantData(Map<String, String> data, String ddpParticipantId, String fieldTypeId) {
        this.setDataAccess(new ParticipantDataDao());
        org.broadinstitute.dsm.model.participant.data.ParticipantData participantData = new org.broadinstitute.dsm.model.participant.data.ParticipantData(dataAccess);
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
                    return fieldSettings.getDefaultValue(fieldSettingsDto.getPossibleValues());
                })
                .orElse("");
    }

    private void setDataAccess(Dao dao) {
        this.dataAccess = dao;
    }

    @Override
    protected boolean setDefaultData() {
//        if (elasticSearchParticipantDto.get)
//        return elasticSearchParticipantDto.getProfile()
//                    .map(esProfile -> {
//                        return
//                        insertGenomicIdForParticipant(ddpParticipantId, hruid);
//                        insertExitStatusForParticipant(ddpParticipantId);
//                        isSelfOrDependentParticipant()
//                    })
//                    .orElseGet(() -> {
//                        logger.info("Participant does not have ES profile yet...");
//                        return false;
//                    });
        return false;
    }
}