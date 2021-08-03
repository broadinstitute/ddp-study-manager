package org.broadinstitute.dsm.model;

import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.ddp.db.TransactionWrapper;
import org.broadinstitute.dsm.db.DDPInstance;
import org.broadinstitute.dsm.db.InstanceSettings;
import org.broadinstitute.dsm.db.dao.ddp.kitrequest.KitRequestDao;
import org.broadinstitute.dsm.db.dao.kit.BSPKitQueryResultDao;
import org.broadinstitute.dsm.db.dto.kit.BSPKitQueryResultDto;
import org.broadinstitute.dsm.db.dto.settings.InstanceSettingsDto;
import org.broadinstitute.dsm.model.bsp.BSPKitInfo;
import org.broadinstitute.dsm.model.bsp.BSPKitStatus;
import org.broadinstitute.dsm.statics.ApplicationConfigConstants;
import org.broadinstitute.dsm.statics.ESObjectConstants;
import org.broadinstitute.dsm.util.ElasticSearchUtil;
import org.broadinstitute.dsm.util.EventUtil;
import org.broadinstitute.dsm.util.NotificationUtil;
import org.broadinstitute.dsm.util.SystemUtil;
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
        BSPKitQueryResultDao bspKitQueryResultDao = new BSPKitQueryResultDao();
        Optional<BSPKitQueryResultDto> bspKitQueryResult = bspKitQueryResultDao.getBSPKitQueryResult(kitLabel);
        Optional<BSPKitStatus> result = Optional.empty();
        if (bspKitQueryResult.isEmpty()) {
            logger.info("No kit w/ label " + kitLabel + " found");
        }
        else {
            BSPKitQueryResultDto maybeBspKitQueryResult = bspKitQueryResult.get();
            if (StringUtils.isNotBlank(maybeBspKitQueryResult.getParticipantExitId())) {
                String message = "Kit of exited participant " + maybeBspKitQueryResult.getBspParticipantId() + " was received by GP.<br>";
                notificationUtil.sentNotification(maybeBspKitQueryResult.getNotificationRecipient(), message, NotificationUtil.DSM_SUBJECT);
                result = Optional.of(new BSPKitStatus(BSPKitStatus.EXITED));
            }
            else if (StringUtils.isNotBlank(maybeBspKitQueryResult.getDeactivationDate())) {
                result = Optional.of(new BSPKitStatus(BSPKitStatus.DEACTIVATED));
            }
        }
        return result;
    }

    public boolean canReceiveKit(@NonNull String kitLabel) {
        BSPKitQueryResultDao bspKitQueryResultDao = new BSPKitQueryResultDao();
        Optional<BSPKitQueryResultDto> bspKitQueryResult = bspKitQueryResultDao.getBSPKitQueryResult(kitLabel);
        if (bspKitQueryResult.isEmpty()) {
            logger.info("No kit w/ label " + kitLabel + " found");
            return false;
        }
        else {
            BSPKitQueryResultDto maybeBspKitQueryResult = bspKitQueryResult.get();
            if (StringUtils.isNotBlank(maybeBspKitQueryResult.getParticipantExitId()) || StringUtils.isNotBlank(maybeBspKitQueryResult.getDeactivationDate())) {
                logger.info("Kit can not be received.");
                return false;
            }
        }
        return true;
    }

    public Optional<BSPKitInfo> receiveBSPKit(String kitLabel, NotificationUtil notificationUtil) {
        logger.info("Trying to receive kit " + kitLabel);
        BSPKitQueryResultDao bspKitQueryResultDao = new BSPKitQueryResultDao();
        Optional<BSPKitQueryResultDto> bspKitQueryResult = bspKitQueryResultDao.getBSPKitQueryResult(kitLabel);
        bspKitQueryResult.map(maybeBspKitQueryResult -> {
            if (StringUtils.isNotBlank(maybeBspKitQueryResult.getDdpParticipantId())) {
                DDPInstance ddpInstance = DDPInstance.getDDPInstance(maybeBspKitQueryResult.getInstanceName());
                InstanceSettings instanceSettings = new InstanceSettings();
                InstanceSettingsDto instanceSettingsDto = instanceSettings.getInstanceSettings(maybeBspKitQueryResult.getInstanceName());
                instanceSettingsDto.getKitBehaviorChange()
                        .flatMap(kitBehavior -> kitBehavior.stream().filter(o -> o.getName().equals(InstanceSettings.INSTANCE_SETTING_RECEIVED)).findFirst())
                        .ifPresentOrElse(received -> {
                            Map<String, Map<String, Object>> participants = ElasticSearchUtil.getFilteredDDPParticipantsFromES(ddpInstance,
                                    ElasticSearchUtil.BY_GUID + maybeBspKitQueryResult.getDdpParticipantId());
                            writeSampleReceivedToES(ddpInstance, maybeBspKitQueryResult);
                            Map<String, Object> participant = participants.get(maybeBspKitQueryResult.getDdpParticipantId());
                            if (participant != null) {
                                boolean triggerDDP = true;
                                boolean specialBehavior = InstanceSettings.shouldKitBehaveDifferently(participant, received);
                                if (specialBehavior) {
                                    //don't trigger ddp to sent out email, only email to study staff
                                    triggerDDP = false;
                                    if (InstanceSettings.TYPE_NOTIFICATION.equals(received.getType())) {
                                        String message = "Kit of participant " + maybeBspKitQueryResult.getBspParticipantId() + " was received by GP. <br> " +
                                                "CollaboratorSampleId:  " + maybeBspKitQueryResult.getBspSampleId() + " <br> " +
                                                received.getValue();
                                        notificationUtil.sentNotification(maybeBspKitQueryResult.getNotificationRecipient(), message, NotificationUtil.UNIVERSAL_NOTIFICATION_TEMPLATE, NotificationUtil.DSM_SUBJECT);
                                    }
                                    else {
                                        logger.error("Instance settings behavior for kit was not known " + received.getType());
                                    }
                                }
                                bspKitQueryResultDao.setKitReceivedAndTriggerDDP(kitLabel, triggerDDP, maybeBspKitQueryResult);
                            }
                        }, () -> {
                            bspKitQueryResultDao.setKitReceivedAndTriggerDDP(kitLabel, true, maybeBspKitQueryResult);
                        });

                String bspParticipantId = maybeBspKitQueryResult.getBspParticipantId();
                String bspSampleId = maybeBspKitQueryResult.getBspSampleId();
                String bspMaterialType = maybeBspKitQueryResult.getBspMaterialType();
                String bspReceptacleType = maybeBspKitQueryResult.getBspReceptacleType();
                int bspOrganism;
                try {
                    bspOrganism = Integer.parseInt(maybeBspKitQueryResult.getBspOrganism());
                }
                catch (NumberFormatException e) {
                    throw new RuntimeException("Organism " + maybeBspKitQueryResult.getBspOrganism() + " can't be parsed to integer", e);
                }

                logger.info("Returning info for kit w/ label " + kitLabel + " for " + maybeBspKitQueryResult.getInstanceName());
                return Optional.of(new BSPKitInfo(maybeBspKitQueryResult.getBspCollection(),
                        bspOrganism,
                        "U",
                        bspParticipantId,
                        bspSampleId,
                        bspMaterialType,
                        bspReceptacleType));
            }
            else {
                throw new RuntimeException("No participant id for " + kitLabel + " from " + maybeBspKitQueryResult.getInstanceName());
            }
        });
        return Optional.empty();
    }

    private void writeSampleReceivedToES(DDPInstance ddpInstance, BSPKitQueryResultDto bspKitInfo) {
        String kitRequestId = new KitRequestDao().getKitRequestIdByBSPParticipantId(bspKitInfo.getBspParticipantId());
        Map<String, Object> nameValuesMap = new HashMap<>();
        nameValuesMap.put(ESObjectConstants.RECEIVED, SystemUtil.getISO8601DateString());
        ElasticSearchUtil.writeSample(ddpInstance, kitRequestId, bspKitInfo.getDdpParticipantId(), ESObjectConstants.SAMPLES,
                ESObjectConstants.KIT_REQUEST_ID, nameValuesMap);
    }

    public void triggerDDP(Connection conn, @NonNull BSPKitQueryResultDto bspKitInfo, boolean firstTimeReceived, String kitLabel) {
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