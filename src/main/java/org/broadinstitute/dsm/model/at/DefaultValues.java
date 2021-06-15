package org.broadinstitute.dsm.model.at;

import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.dsm.db.DDPInstance;
import org.broadinstitute.dsm.db.ParticipantData;
import org.broadinstitute.dsm.db.dao.ddp.participant.ParticipantDataDao;
import org.broadinstitute.dsm.model.participant.data.NewParticipantData;
import org.broadinstitute.dsm.util.ElasticSearchUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class DefaultValues {

    private static final Logger logger = LoggerFactory.getLogger(DefaultValues.class);

    private static final String FIELD_TYPE_ID = "AT_GROUP_GENOME_STUDY";
    private static final String GENOME_STUDY_CPT_ID = "GENOME_STUDY_CPT_ID";
    private static final String PREFIX = "DDP_ATCP_";

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
            if (participantDataList == null) {
                Map<String, Object> esParticipantData = entry.getValue();
                Map<String, Object> profile = (Map<String, Object>) esParticipantData.get(ElasticSearchUtil.PROFILE);
//                TODO only for `registration_type` = `self` && `dependent`
                List<Map<String, Object>> answers = ((List<Map<String, Object>>) esParticipantData.get("activities")).stream()
                        .filter(f -> "PREQUAL".equals(f.get("activityCode")))
                        .map(m -> (List<Map<String, Object>>) m.get("questionsAnswers"))
                        .findFirst()
                        .orElse(Collections.emptyList());
                boolean isSelfOrDependent = answers.stream()
                        .filter(f -> "PREQUAL_SELF_DESCRIBE".equals(f.get("stableId")))
                        .anyMatch(f -> ((List) f.get("answer")).stream().filter(f2 -> "DIAGNOSED".equals(f2) || "CHILD_DIAGNOSED".equals(f2)).findFirst().isPresent());
                String hruid = (String) profile.get(ElasticSearchUtil.HRUID);
                if (StringUtils.isNotBlank(hruid) && isSelfOrDependent)  {
                    NewParticipantData newParticipantData = new NewParticipantData(participantDataDao);
                    Map<String, String> dataMap = new HashMap<>();
                    dataMap.put(GENOME_STUDY_CPT_ID, PREFIX.concat(hruid));
                    newParticipantData.setData(ddpParticipantId, Integer.parseInt(instance.getDdpInstanceId()),
                            FIELD_TYPE_ID, dataMap);
                    newParticipantData.insertParticipantData("SYSTEM");
                    logger.info(GENOME_STUDY_CPT_ID + " was created for participant with id: " + ddpParticipantId + " at " + FIELD_TYPE_ID);
                    addedNewParticipantData = true;
                }
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
}
