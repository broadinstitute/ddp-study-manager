package org.broadinstitute.dsm.route;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.dsm.db.*;
import org.broadinstitute.dsm.db.dao.bookmark.BookmarkDao;
import org.broadinstitute.dsm.db.dao.ddp.tissue.TissueSMIDDao;
import org.broadinstitute.dsm.db.dao.kit.BSPDummyKitDao;
import org.broadinstitute.dsm.model.elasticsearch.ESProfile;
import org.broadinstitute.dsm.model.elasticsearch.ElasticSearchParticipantDto;
import org.broadinstitute.dsm.statics.RequestParameter;
import org.broadinstitute.dsm.util.ElasticSearchUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;
import spark.Route;

import java.util.List;
import java.util.Optional;
import java.util.Random;

public class CreateClinicalDummyKitRoute implements Route {
    private static final Logger logger = LoggerFactory.getLogger(CreateClinicalDummyKitRoute.class);
    private static String CLINICAL_KIT_REALM = "CLINICAL_KIT_REALM";
    private static String CLINICAL_KIT_PREFIX = "CLINICALKIT_";
    private int REALM;
    private static final String USER_ID = "MERCURY";
    private final String FFPE_USER = "ffpe-dummy-kit-creator";
    private final String FFPE = "ffpe";
    private final String FFPE_SCROLL = "ffpe-scroll";
    private final String FFPE_SSECTION = "ffpe-section";

    @Override
    public Object handle(Request request, Response response) {
        String kitLabel = request.params(RequestParameter.LABEL);
        String kitTypeString = request.params(RequestParameter.KIT_TYPE);
        if (StringUtils.isBlank(kitLabel)) {
            logger.warn("Got a create Clinical Kit request without a kit label!!");
            response.status(500);
            return "Please include a kit label as a path parameter";
        }
        logger.info("Got a new Clinical Kit request with kit label " + kitLabel + " and kit type " + kitTypeString);
        new BookmarkDao().getBookmarkByInstance(CLINICAL_KIT_REALM).ifPresentOrElse(book -> {
            REALM = (int) book.getValue();
        }, () -> {
            throw new RuntimeException("Bookmark doesn't exist for " + CLINICAL_KIT_REALM);
        });
        DDPInstance ddpInstance = DDPInstance.getDDPInstanceById(REALM);
        BSPDummyKitDao bspDummyKitDao = new BSPDummyKitDao();
        if (ddpInstance != null) {
            List<KitType> kitTypes = KitType.getKitTypes(ddpInstance.getName(), null);
            KitType desiredKitType = kitTypes.stream().filter(k -> kitTypeString.equalsIgnoreCase(k.getName())).findFirst().orElseThrow();
            logger.info("Found kit type " + desiredKitType.getName());
            if (kitTypeString.toLowerCase().indexOf(FFPE) == -1) {
                String ddpParticipantId = bspDummyKitDao.getRandomParticipantForStudy(ddpInstance);
                String kitRequestId = CLINICAL_KIT_PREFIX + KitRequestShipping.createRandom(20);
                Optional<ElasticSearchParticipantDto> maybeParticipantByParticipantId = ElasticSearchUtil.getParticipantESDataByParticipantId(ddpInstance.getParticipantIndexES(), ddpParticipantId);
                maybeParticipantByParticipantId.ifPresentOrElse(p -> {
                    String participantCollaboratorId = KitRequestShipping.getCollaboratorParticipantId(ddpInstance.getBaseUrl(), ddpInstance.getDdpInstanceId(), ddpInstance.isMigratedDDP(),
                            ddpInstance.getCollaboratorIdPrefix(), ddpParticipantId, p.getProfile().map(ESProfile::getHruid).orElseThrow(), null);
                    String collaboratorSampleId = KitRequestShipping.getCollaboratorSampleId(desiredKitType.getKitId(), participantCollaboratorId, desiredKitType.getName());
                    logger.info("Found collaboratorSampleId  " + collaboratorSampleId);
                    //if instance not null
                    String dsmKitRequestId = KitRequestShipping.writeRequest(ddpInstance.getDdpInstanceId(), kitRequestId, desiredKitType.getKitId(),
                            ddpParticipantId, participantCollaboratorId, collaboratorSampleId,
                            USER_ID, "", "", "", false, "");
                    bspDummyKitDao.updateKitLabel(kitLabel, dsmKitRequestId);

                }, () -> {
                    throw new RuntimeException(" Participant " + ddpParticipantId + " was not found!");
                });
            }else{
                String smIdType;
                if(kitTypeString.equalsIgnoreCase(FFPE_SCROLL)){
                    smIdType = TissueSmId.SCROLLS;
                }else if(kitTypeString.equalsIgnoreCase(FFPE_SSECTION)){
                    smIdType = TissueSmId.USS;
                }else{
                    throw new RuntimeException("The FFPE kit type does not match any of the valid types "+kitTypeString);
                }
                Optional<String> randomOncHistoryDetailId = bspDummyKitDao.getRandomOncHistoryForStudy(ddpInstance.getName());
                randomOncHistoryDetailId.ifPresentOrElse(oid -> {
                            List<Tissue> tissueIds = OncHistoryDetail.getOncHistoryDetail(oid, ddpInstance.getName()).getTissues();
                            String tissueId;

                            if (tissueIds.isEmpty()){
                                tissueId = Tissue.createNewTissue(oid, FFPE_USER);
                            }else{
                                tissueId = tissueIds.get( new Random().nextInt(tissueIds.size())).getTissueId();
                            }
                            new TissueSMIDDao().createNewSMIDForTissue(tissueId, FFPE_USER, smIdType);
                        },
                        ()-> {throw new RuntimeException("no onc history for study " + ddpInstance.getName() + " was found!");});
            }
            logger.info("Kit added successfully");
            response.status(200);
            return null;

        }
        logger.error("Error occurred while adding kit");
        response.status(500);
        return null;
    }

}
