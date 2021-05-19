package org.broadinstitute.dsm.model.at;

import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.dsm.db.DDPInstance;
import org.broadinstitute.dsm.db.ParticipantData;
import org.broadinstitute.dsm.db.dao.participant.data.ParticipantDataDao;
import org.broadinstitute.dsm.model.participant.data.NewParticipantData;
import org.broadinstitute.dsm.util.ElasticSearchUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DefaultValues {

    private static final Logger logger = LoggerFactory.getLogger(DefaultValues.class);

    private static final String FIELD_TYPE_ID = "AT_GROUP_GENOME_STUDY";
    private static final String GENOME_STUDY_CPT_ID = "GENOME_STUDY_CPT_ID";
    private static final String PREFIX = "DDP_ATCP_";

    public static Map<String, List<ParticipantData>> addDefaultValues(Map<String, List<ParticipantData>> participantData,
                                        Map<String, Map<String, Object>> participantESData, @NonNull DDPInstance instance) {
        if (participantESData == null) {
            logger.warn("Could not update default values, participant ES data is null");
            return participantData;
        }
        for (String ddpParticipantId : participantESData.keySet()) {
            List<ParticipantData> participantDataList = participantData.get(ddpParticipantId);
            //only needed for new signups - migrated pts already have the values needed
            if (participantDataList == null) {
                Map<String, Object> esParticipantData = participantESData.get(ddpParticipantId);
                Map<String, Object> profile = (Map<String, Object>) esParticipantData.get(ElasticSearchUtil.PROFILE);
                String hruid = (String) profile.get(ElasticSearchUtil.HRUID);
                if (StringUtils.isNotBlank(hruid)) {
                    ParticipantDataDao participantDataDao = new ParticipantDataDao();
                    NewParticipantData newParticipantData = new NewParticipantData(participantDataDao);
                    Map<String, String> dataMap = new HashMap<>();
                    dataMap.put(GENOME_STUDY_CPT_ID, PREFIX.concat(hruid));
                    newParticipantData.setData(ddpParticipantId, Integer.parseInt(instance.getDdpInstanceId()),
                            FIELD_TYPE_ID, dataMap);
                    newParticipantData.insertParticipantData("SYSTEM");
                    logger.info(GENOME_STUDY_CPT_ID + " was created for participant with id: " + ddpParticipantId + " at " + FIELD_TYPE_ID);
                }
            }
        }
        return participantData;
    }
}
