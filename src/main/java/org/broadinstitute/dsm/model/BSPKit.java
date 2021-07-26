package org.broadinstitute.dsm.model;

import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.ddp.db.TransactionWrapper;
import org.broadinstitute.dsm.db.DDPInstance;
import org.broadinstitute.dsm.db.InstanceSettings;
import org.broadinstitute.dsm.db.dao.ddp.kitrequest.KitRequestDao;
import org.broadinstitute.dsm.db.dto.settings.InstanceSettingsDto;
import org.broadinstitute.dsm.model.bsp.BSPKitInfo;
import org.broadinstitute.dsm.model.bsp.BSPKitQueryResult;
import org.broadinstitute.dsm.model.bsp.BSPKitStatus;
import org.broadinstitute.dsm.statics.ApplicationConfigConstants;
import org.broadinstitute.dsm.statics.ESObjectConstants;
import org.broadinstitute.dsm.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class BSPKit {
    private static Logger logger = LoggerFactory.getLogger(BSPKit.class);

    public Optional<BSPKitStatus> getKitStatus(@NonNull String kitLabel, NotificationUtil notificationUtil) {
        logger.info("Checking label " + kitLabel);
        BSPKitQueryResult bspKitQueryResult = BSPKitQueryResult.getBSPKitQueryResult(kitLabel);
        Optional<BSPKitStatus> result = Optional.empty();
        if (bspKitQueryResult == null) {
            logger.info("No kit w/ label " + kitLabel + " found");
        }
        else if (StringUtils.isNotBlank(bspKitQueryResult.getParticipantExitId())) {
            String message = "Kit of exited participant " + bspKitQueryResult.getBspParticipantId() + " was received by GP.<br>";
            notificationUtil.sentNotification(bspKitQueryResult.getNotificationRecipient(), message, NotificationUtil.DSM_SUBJECT);
            result= Optional.of(new BSPKitStatus(BSPKitStatus.EXITED));
        }
        else if (StringUtils.isNotBlank(bspKitQueryResult.getDeactivationDate())) {
            return Optional.of(new BSPKitStatus(BSPKitStatus.DEACTIVATED));
        }
        return result;
    }

    public boolean canReceiveKit(@NonNull String kitLabel) {
        BSPKitQueryResult bspKitQueryResult = BSPKitQueryResult.getBSPKitQueryResult(kitLabel);
        if (bspKitQueryResult == null) {
            logger.info("No kit w/ label " + kitLabel + " found");
            return false;
        }
        if (StringUtils.isNotBlank(bspKitQueryResult.getParticipantExitId()) || StringUtils.isNotBlank(bspKitQueryResult.getDeactivationDate())) {
            logger.info("Kit can not be received.");
            return false;
        }
        return true;

    }

    public BSPKitInfo receiveBSPKit(String kitLabel, NotificationUtil notificationUtil) {
        logger.info("Trying to receive kit "+kitLabel);
        return TransactionWrapper.inTransaction(conn -> {
            BSPKitQueryResult bspKitQueryResult = BSPKitQueryResult.getBSPKitQueryResult(kitLabel);
            if (StringUtils.isNotBlank(bspKitQueryResult.getDdpParticipantId())) {
                boolean firstTimeReceived = KitUtil.setKitReceived(conn, kitLabel);
                DDPInstance ddpInstance = DDPInstance.getDDPInstance(bspKitQueryResult.getInstanceName());
                InstanceSettings instanceSettings = new InstanceSettings();
                InstanceSettingsDto instanceSettingsDto = instanceSettings.getInstanceSettings(bspKitQueryResult.getInstanceName());
                Value received = instanceSettingsDto
                        .getKitBehaviorChange()
                        .map(kitBehavior -> kitBehavior.stream().filter(o -> o.getName().equals(InstanceSettings.INSTANCE_SETTING_RECEIVED)).findFirst().orElse(null))
                        .orElse(null);

                if (received != null) {
                    Map<String, Map<String, Object>> participants = ElasticSearchUtil.getFilteredDDPParticipantsFromES(ddpInstance,
                            ElasticSearchUtil.BY_GUID + bspKitQueryResult.getDdpParticipantId());
                    writeSampleReceivedToES(ddpInstance, bspKitQueryResult);
                    Map<String, Object> participant = participants.get(bspKitQueryResult.getDdpParticipantId());
                    if (participant != null) {
                        boolean specialBehavior = InstanceSettings.shouldKitBehaveDifferently(participant, received);
                        if (specialBehavior) {
                            //don't trigger ddp to sent out email, only email to study staff
                            if (InstanceSettings.TYPE_NOTIFICATION.equals(received.getType())) {
                                String message = "Kit of participant " + bspKitQueryResult.getBspParticipantId() + " was received by GP. <br> " +
                                        "CollaboratorSampleId:  " + bspKitQueryResult.getBspSampleId() + " <br> " +
                                        received.getValue();
                                notificationUtil.sentNotification(bspKitQueryResult.getNotificationRecipient(), message, NotificationUtil.UNIVERSAL_NOTIFICATION_TEMPLATE, NotificationUtil.DSM_SUBJECT);
                            }
                            else {
                                logger.error("Instance settings behavior for kit was not known " + received.getType());
                            }
                        }
                        else {
                            triggerDDP(conn, bspKitQueryResult, firstTimeReceived, kitLabel);
                        }
                    }
                }
                else {
                    triggerDDP(conn, bspKitQueryResult, firstTimeReceived, kitLabel);
                }

                String bspParticipantId = bspKitQueryResult.getBspParticipantId();
                String bspSampleId = bspKitQueryResult.getBspSampleId();
                String bspMaterialType = bspKitQueryResult.getBspMaterialType();
                String bspReceptacleType = bspKitQueryResult.getBspReceptacleType();
                int bspOrganism;
                try {
                    bspOrganism = Integer.parseInt(bspKitQueryResult.getBspOrganism());
                }
                catch (NumberFormatException e) {
                    throw new RuntimeException("Organism " + bspKitQueryResult.getBspOrganism() + " can't be parsed to integer", e);
                }

                logger.info("Returning info for kit w/ label " + kitLabel + " for " + bspKitQueryResult.getInstanceName());
                return new BSPKitInfo(bspKitQueryResult.getBspCollection(),
                        bspOrganism,
                        "U",
                        bspParticipantId,
                        bspSampleId,
                        bspMaterialType,
                        bspReceptacleType);
            }
            else {
                throw new RuntimeException("No participant id for " + kitLabel + " from " + bspKitQueryResult.getInstanceName());
            }
        });
    }

    private void writeSampleReceivedToES(DDPInstance ddpInstance, BSPKitQueryResult bspKitInfo) {
        String kitRequestId = new KitRequestDao().getKitRequestIdByBSPParticipantId(bspKitInfo.getBspParticipantId());
        Map<String, Object> nameValuesMap = new HashMap<>();
        nameValuesMap.put(ESObjectConstants.RECEIVED, SystemUtil.getISO8601DateString());
        ElasticSearchUtil.writeSample(ddpInstance, kitRequestId, bspKitInfo.getDdpParticipantId(), ESObjectConstants.SAMPLES,
                ESObjectConstants.KIT_REQUEST_ID, nameValuesMap);
    }

    private void triggerDDP(Connection conn, @NonNull BSPKitQueryResult bspKitInfo, boolean firstTimeReceived, String kitLabel) {
        try {
            if (bspKitInfo.isHasParticipantNotifications() && firstTimeReceived) {
                KitDDPNotification kitDDPNotification = KitDDPNotification.getKitDDPNotification(TransactionWrapper.getSqlFromConfig(ApplicationConfigConstants.GET_RECEIVED_KIT_INFORMATION_FOR_NOTIFICATION_EMAIL), kitLabel, 1);
                if (kitDDPNotification != null) {
                    EventUtil.triggerDDP(conn, kitDDPNotification);
                }
            }
        }
        catch (Exception e) {
            logger.error("Failed doing DSM internal received things for kit w/ label " + kitLabel, e);
        }
    }
}
