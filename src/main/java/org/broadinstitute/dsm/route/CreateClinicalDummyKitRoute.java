package org.broadinstitute.dsm.route;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.ddp.handlers.util.Result;
import org.broadinstitute.dsm.db.DDPInstance;
import org.broadinstitute.dsm.db.KitRequestShipping;
import org.broadinstitute.dsm.db.KitType;
import org.broadinstitute.dsm.db.dao.kit.BSPKitDao;
import org.broadinstitute.dsm.statics.RequestParameter;
import org.broadinstitute.dsm.util.DBUtil;
import org.broadinstitute.dsm.util.ElasticSearchUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;
import spark.Route;

import java.util.List;
import java.util.Map;

public class CreateClinicalDummyKitRoute implements Route {
    private static final Logger logger = LoggerFactory.getLogger(CreateClinicalDummyKitRoute.class);
    private static String CLINICAL_KIT_REALM = "CLINICAL_KIT_REALM";
    private static String CLINICAL_KIT_PREFIX = "CLINICALKIT_";
    private int REALM;
    private static final String USER_ID = "MERCURY";

    @Override
    public Object handle(Request request, Response response) {
        String kitLabel = request.params(RequestParameter.LABEL);
        if (StringUtils.isBlank(kitLabel)) {
            logger.warn("Got a create Clinical Kit request without a kit label!!");
            return new Result(500, "Please include a kit label as a path parameter");
        }
        logger.info("Got a new Clinical Kit request with kit label " + kitLabel);
        REALM = (int) DBUtil.getBookmark(CLINICAL_KIT_REALM);
        KitType salivaKitType = null;
        DDPInstance ddpInstance = DDPInstance.getDDPInstanceById(REALM);
        if (ddpInstance != null) {
            List<KitType> kitTypeIds = KitType.getKitTypes(ddpInstance.getName(), null);

            for (KitType kitType : kitTypeIds) {
                if (kitType.getName().equals("SALIVA")) {
                    salivaKitType = kitType;
                    break;
                }
            }
            if (salivaKitType != null) {
                logger.info("Got instance  " + ddpInstance.getName() + " and kit type id " + salivaKitType.getKitId());
            }
            String kitRequestId = CLINICAL_KIT_PREFIX + KitRequestShipping.createRandom(20);

            String ddpParticipantId = BSPKitDao.getRandomParticipantIdForStudy(ddpInstance.getDdpInstanceId());
            Map<String, Map<String, Object>> participantsESData = ElasticSearchUtil.getFilteredDDPParticipantsFromES(ddpInstance, ElasticSearchUtil.BY_GUID + ddpParticipantId);
            if (participantsESData != null && !participantsESData.isEmpty()) {
                Map<String, Object> participantESData = participantsESData.get(ddpParticipantId);
                if (participantESData != null && !participantESData.isEmpty()) {
                    Map<String, Object> profile = (Map<String, Object>) participantESData.get(ElasticSearchUtil.PROFILE);
                    String participantCollaboratorId = KitRequestShipping.getCollaboratorParticipantId(ddpInstance.getBaseUrl(), ddpInstance.getDdpInstanceId(), ddpInstance.isMigratedDDP(),
                            ddpInstance.getCollaboratorIdPrefix(), ddpParticipantId, (String) profile.get("hruid"), null);
                    String collaboratorSampleId = BSPKitDao.getCollaboratorSampleId(salivaKitType.getKitId(), participantCollaboratorId, salivaKitType.getName());
                    if (ddpParticipantId != null) {
                        //if instance not null
                        String dsmKitRequestId = KitRequestShipping.writeRequest(ddpInstance.getDdpInstanceId(), kitRequestId, salivaKitType.getKitId(),
                                ddpParticipantId, participantCollaboratorId, collaboratorSampleId,
                                USER_ID, "", "", "", false, "");
                        BSPKitDao.updateKitLabel(kitLabel, dsmKitRequestId);
                    }
                    logger.info("Kit added successfully");
                    response.status(200);
                    return response;
                }
                logger.error("Participants for realm " + ddpInstance.getName() + " not found in ES");

            }
            logger.error("Participant " + ddpParticipantId + " was not found in ES!");
        }
        logger.error("Error occurred while adding kit");
        response.status(500);
        return response;
    }

}
