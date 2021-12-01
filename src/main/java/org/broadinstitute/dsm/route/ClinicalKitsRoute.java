package org.broadinstitute.dsm.route;

import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.dsm.db.DDPInstance;
import org.broadinstitute.dsm.db.dao.kit.BSPKitDao;
import org.broadinstitute.dsm.db.dto.kit.BSPKitDto;
import org.broadinstitute.dsm.db.dto.kit.ClinicalKitDto;
import org.broadinstitute.dsm.model.BSPKit;
import org.broadinstitute.dsm.model.bsp.BSPKitInfo;
import org.broadinstitute.dsm.model.bsp.BSPKitStatus;
import org.broadinstitute.dsm.statics.RequestParameter;
import org.broadinstitute.dsm.util.NotificationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;
import spark.Route;

import java.util.Optional;

public class ClinicalKitsRoute implements Route {

    private static final Logger logger = LoggerFactory.getLogger(ClinicalKitsRoute.class);

    private NotificationUtil notificationUtil;

    public ClinicalKitsRoute(@NonNull NotificationUtil notificationUtil) {
        this.notificationUtil = notificationUtil;
    }

    @Override
    public Object handle(Request request, Response response) {
        String kitLabel = request.params(RequestParameter.LABEL);
        if (StringUtils.isBlank(kitLabel)) {
            throw new RuntimeException("Please include a kit label as a path parameter");
        }
        BSPKit bspKit = new BSPKit();
        if (!bspKit.canReceiveKit(kitLabel)) {
            Optional<BSPKitStatus> result = bspKit.getKitStatus(kitLabel, notificationUtil);
            if(!result.isEmpty()){
                return result.get();
            }

        }
        return getClinicalKit(kitLabel);
    }

    private ClinicalKitDto getClinicalKit(String kitLabel) {
        logger.info("Checking label " + kitLabel);
        BSPKitDao bspKitDao = new BSPKitDao();
        // this method already sets the received time, check for exited and deactivation and special behaviour, and triggers DDP, we don't need a new
        BSPKit bspKit = new BSPKit();
        ClinicalKitDto clinicalKit = new ClinicalKitDto();
        Optional<BSPKitInfo> maybeKitInfo = bspKit.receiveBSPKit(kitLabel, notificationUtil);
        maybeKitInfo.ifPresent(kitInfo -> {
            logger.info("Creating clinical kit to return to GP " + kitLabel);
            clinicalKit.setCollaboratorParticipantId(kitInfo.getCollaboratorParticipantId());
            clinicalKit.setSampleId(kitInfo.getCollaboratorSampleId());
            clinicalKit.setMaterialType(kitInfo.getMaterialInfo());
            clinicalKit.setVesselType(kitInfo.getReceptacleName());
            clinicalKit.setSampleType(kitInfo.getKitType());
            clinicalKit.setMfBarcode(kitLabel);
            clinicalKit.setSampleCollection(kitInfo.getSampleCollectionBarcode());
            Optional<BSPKitDto> bspKitQueryResult = bspKitDao.getBSPKitQueryResult(kitLabel);
            bspKitQueryResult.orElseThrow(() -> {throw new RuntimeException("kit label was not found "+kitLabel);});
            BSPKitDto maybeBspKitQueryResult = bspKitQueryResult.get();
            DDPInstance ddpInstance = DDPInstance.getDDPInstance(maybeBspKitQueryResult.getInstanceName());
            clinicalKit.setNecessaryParticipantDataToClinicalKit( maybeBspKitQueryResult.getDdpParticipantId(), ddpInstance);
        });
        if(maybeKitInfo.isEmpty()){
            return clinicalKit.getClinicalKitBasedONSmId(kitLabel);
        }
        return clinicalKit;

    }

}
