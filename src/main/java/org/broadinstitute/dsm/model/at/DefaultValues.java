package org.broadinstitute.dsm.model.at;

import com.google.gson.Gson;
import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.dsm.db.DDPInstance;
import org.broadinstitute.dsm.db.ParticipantData;
import org.broadinstitute.dsm.db.dao.ddp.participant.ParticipantDataDao;
import org.broadinstitute.dsm.model.participant.data.NewParticipantData;
import org.broadinstitute.dsm.util.ElasticSearchUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class DefaultValues {

    private static final Logger logger = LoggerFactory.getLogger(DefaultValues.class);

    private static final String FIELD_TYPE_ID = "AT_GROUP_GENOME_STUDY";
    private static final String GENOME_STUDY_CPT_ID = "GENOME_STUDY_CPT_ID";
    private static final String PREFIX = "DDP_ATCP_";
    private static final String REGISTRATION_TYPE = "REGISTRATION_TYPE";
    public static final String REGISTRATION_TYPE_SELF = "self";
    public static final String REGISTRATION_TYPE_DEPENDENT = "dependent";

    public static Map<String, List<ParticipantData>> addDefaultValues(Map<String, List<ParticipantData>> participantData,
                                        Map<String, Map<String, Object>> participantESData, @NonNull DDPInstance instance, String queryAddition) {
        if (participantESData == null) {
            logger.warn("Could not update default values, participant ES data is null");
            return participantData;
        }
        boolean addedNewParticipantData = false;
        ParticipantDataDao participantDataDao = new ParticipantDataDao();
        for (Map.Entry<String, Map<String, Object>> entry: participantESData.entrySet()) {
            String ddpParticipantId = entry.getKey();
            List<ParticipantData> participantDataList = participantData.get(ddpParticipantId);
            //only needed for new signups - migrated pts already have the values needed
            if (participantDataList == null) continue;

            if (hasParticipantDataGenomicId(participantDataList)) continue;

            if (isSelfOrDependentParticipant(participantDataList)) {
                Map<String, Object> esParticipantData = entry.getValue();
                Map<String, Object> profile = (Map<String, Object>) esParticipantData.get(ElasticSearchUtil.PROFILE);
                String hruid = (String) profile.get(ElasticSearchUtil.HRUID);

                addedNewParticipantData = getParticipantGenomicFieldData(participantDataList)
                        .map(pData -> updateGenomicIdForParticipant(instance, participantDataDao, ddpParticipantId, hruid, pData))
                        .orElseGet(() -> insertGenomicIdForParticipant(instance, participantDataDao, ddpParticipantId, hruid));
            }
        }
        if (addedNewParticipantData) {
            //participant data was added, getting new list of data
            if (StringUtils.isNotBlank(queryAddition)) {
                participantData = ParticipantData.getParticipantData(instance.getName(), queryAddition);
            }
            else {
                participantData = ParticipantData.getParticipantData(instance.getName());
            }
        }
        return participantData;
    }

    private static boolean updateGenomicIdForParticipant(DDPInstance instance, ParticipantDataDao participantDataDao, String ddpParticipantId, String hruid,
                                       ParticipantData pData) {
        if (StringUtils.isBlank(hruid)) return false;
        NewParticipantData newParticipantData = new NewParticipantData(participantDataDao);
        Map<String, String> dataMap = new Gson().fromJson(pData.getData(), Map.class);
        dataMap.put(GENOME_STUDY_CPT_ID, PREFIX.concat(hruid));
        newParticipantData.setData(ddpParticipantId, Integer.parseInt(instance.getDdpInstanceId()), pData.getFieldTypeId(), dataMap);
        return newParticipantData.updateParticipantData(Integer.parseInt(pData.getDataId()), "SYSTEM");
    }

    private static boolean insertGenomicIdForParticipant(DDPInstance instance, ParticipantDataDao participantDataDao, String ddpParticipantId, String hruid) {
        if (StringUtils.isBlank(hruid)) return false;
        NewParticipantData newParticipantData = new NewParticipantData(participantDataDao);
        Map<String, String> dataMap = Map.of(GENOME_STUDY_CPT_ID, PREFIX.concat(hruid));
        newParticipantData.setData(ddpParticipantId, Integer.parseInt(instance.getDdpInstanceId()),
                FIELD_TYPE_ID, dataMap);
        newParticipantData.insertParticipantData("SYSTEM");
        logger.info(GENOME_STUDY_CPT_ID + " was created for participant with id: " + ddpParticipantId + " at " + FIELD_TYPE_ID);
        return true;
    }

    private static boolean isSelfOrDependentParticipant(List<ParticipantData> participantDataList) {
        if (participantDataList.isEmpty()) return false;
        return participantDataList.stream()
                .anyMatch(pData -> {
                    Map<String, String> data = new Gson().fromJson(pData.getData(), Map.class);
                    return data.containsKey(REGISTRATION_TYPE) &&
                            (REGISTRATION_TYPE_SELF.equalsIgnoreCase(data.get(REGISTRATION_TYPE))
                            || REGISTRATION_TYPE_DEPENDENT.equalsIgnoreCase(data.get(REGISTRATION_TYPE)));
                });
    }

    private static boolean hasParticipantDataGenomicId(List<ParticipantData> participantDataList) {
        if (participantDataList.isEmpty()) return false;
        return participantDataList.stream()
                .anyMatch(participantData -> {
                    Map<String, String> data = new Gson().fromJson(participantData.getData(), Map.class);
                    return data.containsKey(GENOME_STUDY_CPT_ID) && StringUtils.isNotBlank(data.get(GENOME_STUDY_CPT_ID));
                });
    }

    private static Optional<ParticipantData> getParticipantGenomicFieldData(List<ParticipantData> participantDataList) {
        if (participantDataList.isEmpty()) return Optional.empty();
        return participantDataList.stream()
                .filter(participantData -> FIELD_TYPE_ID.equals(participantData.getFieldTypeId()))
                .findFirst();
    }
 }
