package org.broadinstitute.dsm.route;

import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.ddp.db.TransactionWrapper;
import org.broadinstitute.ddp.email.Recipient;
import org.broadinstitute.dsm.model.bsp.BSPKitQueryResult;
import org.broadinstitute.dsm.model.bsp.BSPKitInfo;
import org.broadinstitute.dsm.model.bsp.BSPKitStatus;
import org.broadinstitute.dsm.model.KitDDPNotification;
import org.broadinstitute.dsm.statics.ApplicationConfigConstants;
import org.broadinstitute.dsm.statics.RequestParameter;
import org.broadinstitute.dsm.util.EventUtil;
import org.broadinstitute.dsm.util.KitUtil;
import org.broadinstitute.dsm.util.NotificationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;
import spark.Route;

import java.util.*;

public class BSPKitQueryRoute implements Route {

    private static final Logger logger = LoggerFactory.getLogger(BSPKitQueryRoute.class);

    public static final String EMAIL_TYPE = "EXITED_KIT_RECEIVED_NOTIFICATION";
    public static final String KITREQUEST_LINK = "/permalink/whereto?";

    private NotificationUtil notificationUtil;

    public BSPKitQueryRoute (@NonNull NotificationUtil notificationUtil) {
        this.notificationUtil = notificationUtil;
    }

    @Override
    public Object handle (Request request, Response response) throws Exception {
        String kitLabel = request.params(RequestParameter.LABEL);
        if (StringUtils.isBlank(kitLabel)) {
            throw new RuntimeException("Please include a kit label as a path parameter");
        }
        return getBSPKitInfo(kitLabel, response);
    }

    public Object getBSPKitInfo (@NonNull String kitLabel, @NonNull Response response) {
        logger.info("Checking label " + kitLabel);
        BSPKitQueryResult bspKitInfo = BSPKitQueryResult.getBSPKitQueryResult(kitLabel);

        if (bspKitInfo == null) {
            logger.info("No kit w/ label " + kitLabel + " found");
            response.status(404);
            return null;
        }

        boolean firstTimeReceived = KitUtil.setKitReceived(kitLabel);
        if (StringUtils.isNotBlank(bspKitInfo.getParticipantExitId())) {
            try {
                String notificationRecipient = bspKitInfo.getNotificationRecipient();
                if (StringUtils.isNotBlank(notificationRecipient)) {
                    notificationRecipient = notificationRecipient.replaceAll("\\s", "");
                    List<String> recipients = Arrays.asList(notificationRecipient.split(","));
                    for (String recipient : recipients) {
                        doNotification(recipient, bspKitInfo.getBspParticipantId());
                    }
                }
            }
            catch (Exception e) {
                logger.error("Was not able to notify study staff.", e);
            }
            return new BSPKitStatus(BSPKitStatus.EXITED);
        }
        if (StringUtils.isNotBlank(bspKitInfo.getDeactivationDate())) {
            return new BSPKitStatus(BSPKitStatus.DEACTIVATED);
        }
        if (StringUtils.isNotBlank(bspKitInfo.getDdpParticipantId())) {
            try {
                if (bspKitInfo.isHasParticipantNotifications() && firstTimeReceived) {
                    KitDDPNotification kitDDPNotification = KitDDPNotification.getKitDDPNotification(TransactionWrapper.getSqlFromConfig(ApplicationConfigConstants.GET_RECEIVED_KIT_INFORMATION_FOR_NOTIFICATION_EMAIL), kitLabel);
                    if (kitDDPNotification != null) {
                        EventUtil.triggerDDP(kitDDPNotification);
                    }
                }
            }
            catch (Exception e) {
                logger.error("Failed doing DSM internal received things for kit w/ label " + kitLabel, e);
            }

            String bspParticipantId = bspKitInfo.getBspParticipantId();
            String bspSampleId = bspKitInfo.getBspSampleId();
            String bspMaterialType = bspKitInfo.getBspMaterialType();
            String bspReceptacleType = bspKitInfo.getBspReceptacleType();
            int bspOrganism;
            try {
                bspOrganism = Integer.parseInt(bspKitInfo.getBspOrganism());
            }
            catch (NumberFormatException e) {
                throw new RuntimeException("Organism " + bspKitInfo.getBspOrganism() + " can't be parsed to integer", e);
            }

            logger.info("Returning info for kit w/ label " + kitLabel + " for " + bspKitInfo.getInstanceName());
            return new BSPKitInfo(bspKitInfo.getBspCollection(),
                    bspOrganism,
                    "U",
                    bspParticipantId,
                    bspSampleId,
                    bspMaterialType,
                    bspReceptacleType);
        }
        else {
            throw new RuntimeException("No participant id for " + kitLabel + " from " + bspKitInfo.getInstanceName());
        }
    }

    private void doNotification (@NonNull String recipient, String bspParticipantId) {
        String message = "Kit of exited participant " + bspParticipantId + " was received by GP.<br>";
        Map<String, String> mapy = new HashMap<>();
        mapy.put(":customText", message);
        Recipient emailRecipient = new Recipient(recipient);
        emailRecipient.setUrl(TransactionWrapper.getSqlFromConfig(ApplicationConfigConstants.EMAIL_FRONTEND_URL_FOR_LINKS) + KITREQUEST_LINK);
        emailRecipient.setSurveyLinks(mapy);
        notificationUtil.queueCurrentAndFutureEmails(EMAIL_TYPE, emailRecipient, EMAIL_TYPE);
    }
}