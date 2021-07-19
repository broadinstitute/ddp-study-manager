package org.broadinstitute.dsm.route;

import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.ddp.db.TransactionWrapper;
import org.broadinstitute.dsm.db.DDPInstance;
import org.broadinstitute.dsm.db.dao.kit.BSPKitDao;
import org.broadinstitute.dsm.db.dto.kit.ClinicalKitDto;
import org.broadinstitute.dsm.model.ParticipantWrapper;
import org.broadinstitute.dsm.model.bsp.BSPKitInfo;
import org.broadinstitute.dsm.model.bsp.BSPKitQueryResult;
import org.broadinstitute.dsm.statics.RequestParameter;
import org.broadinstitute.dsm.util.ElasticSearchUtil;
import org.broadinstitute.dsm.util.NotificationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;
import spark.Route;

import java.sql.Connection;
import java.util.Map;
import java.util.Optional;

public class ClinicalKitsRoute implements Route {
    private String FIRSTNAME = "firstName";
    private String LASTNAME = "lastName";
    private String DATE_OF_BIRtH = "dateOfBirth";

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

        return TransactionWrapper.inTransaction(conn -> {
            return getClinicalKit(conn, kitLabel, response);
        });
    }

    private ClinicalKitDto getClinicalKit(Connection conn, String kitLabel, Response response) {
        logger.info("Checking label " + kitLabel);

        // this method already sets the received time, check for exited and deactivation and special behaviour, and triggers DDP, we don't need a new
        BSPKitInfo kitInfo = (BSPKitInfo) new BSPKitDao().getBSPKitInfo(kitLabel, response, notificationUtil);

        ClinicalKitDto clinicalKit = new ClinicalKitDto(kitInfo.getCollaboratorParticipantId(),
                kitInfo.getCollaboratorSampleId(),
                kitInfo.getSampleCollectionBarcode(),
                kitInfo.getMaterialInfo(),
                kitInfo.getReceptacleName(),
                null,
                null);

        BSPKitQueryResult bspKitQueryResult = BSPKitQueryResult.getBSPKitQueryResult(kitLabel);
        DDPInstance ddpInstance = DDPInstance.getDDPInstance(bspKitQueryResult.getInstanceName());
        String hruid = bspKitQueryResult.getBspParticipantId();
        if (hruid.indexOf('_') != -1) {
            hruid = bspKitQueryResult.getBspParticipantId().substring(bspKitQueryResult.getBspParticipantId().lastIndexOf('_') + 1);
        }
        Optional<ParticipantWrapper> maybeParticipant = ParticipantWrapper.getParticipantFromESByHruid(ddpInstance, hruid);
        maybeParticipant.ifPresentOrElse(p -> {
                    Map<String, String> dsm = (Map<String, String>) p.getData().get(ElasticSearchUtil.DSM);
                    if (dsm != null && !dsm.isEmpty()) {
                        clinicalKit.setDateOfBirth(dsm.get(DATE_OF_BIRtH));
                    }
                    Map<String, String> participantProfileFromEs = (Map<String, String>) p.getData().get(ElasticSearchUtil.PROFILE);
                    String firstName = participantProfileFromEs.get(FIRSTNAME);
                    String lastName = participantProfileFromEs.get(LASTNAME);
                    String mailToName = firstName + " " + lastName;
                    clinicalKit.setMailToName(mailToName);
                }

        ,() -> new RuntimeException("Participant doesn't exist / is not valid for kit " + kitLabel));
        return clinicalKit;

    }
}
