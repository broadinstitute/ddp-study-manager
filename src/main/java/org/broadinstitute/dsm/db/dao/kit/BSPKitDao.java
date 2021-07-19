package org.broadinstitute.dsm.db.dao.kit;

import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.ddp.db.SimpleResult;
import org.broadinstitute.ddp.db.TransactionWrapper;
import org.broadinstitute.dsm.db.DDPInstance;
import org.broadinstitute.dsm.db.InstanceSettings;
import org.broadinstitute.dsm.db.KitRequestShipping;
import org.broadinstitute.dsm.db.dao.Dao;
import org.broadinstitute.dsm.db.dao.ddp.kitrequest.KitRequestDao;
import org.broadinstitute.dsm.db.dto.settings.InstanceSettingsDto;
import org.broadinstitute.dsm.model.KitDDPNotification;
import org.broadinstitute.dsm.model.Value;
import org.broadinstitute.dsm.model.bsp.BSPKitInfo;
import org.broadinstitute.dsm.model.bsp.BSPKitQueryResult;
import org.broadinstitute.dsm.model.bsp.BSPKitStatus;
import org.broadinstitute.dsm.statics.ApplicationConfigConstants;
import org.broadinstitute.dsm.statics.DBConstants;
import org.broadinstitute.dsm.statics.ESObjectConstants;
import org.broadinstitute.dsm.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Response;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.broadinstitute.ddp.db.TransactionWrapper.inTransaction;

public class BSPKitDao implements Dao<BSPKitDao> {

    private static final String SQL_UPDATE_DUMMY_KIT = "UPDATE ddp_kit SET kit_label = ? where dsm_kit_request_id = ?";
    private static final String SQL_SELECT_RANDOM_PT = "SELECT ddp_participant_id FROM ddp_kit_request where ddp_instance_id = ?  ORDER BY RAND() LIMIT 1";
    private static final Logger logger = LoggerFactory.getLogger(BSPKitDao.class);

    public static void updateKitLabel(String kitLabel, String dsmKitRequestId) {
        SimpleResult results = inTransaction((conn) -> {
            SimpleResult dbVals = new SimpleResult();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_UPDATE_DUMMY_KIT)) {
                stmt.setString(1, kitLabel);
                stmt.setString(2, dsmKitRequestId);
                int result = stmt.executeUpdate();
                if (result == 1) {
                    logger.info("Updated dummy kit, set KitLabel " + kitLabel + " for kit with dsmKitRequestId " + dsmKitRequestId);
                }
                else {
                    throw new RuntimeException("Error updating kit  label for " + dsmKitRequestId + " updated " + result + " rows");
                }
            }
            catch (SQLException e) {
                dbVals.resultException = e;
            }
            return dbVals;
        });
        if (results.resultException != null) {
            throw new RuntimeException("Error updating kit  label for " + dsmKitRequestId, results.resultException);
        }
    }

    public static String getRandomParticipantIdForStudy(String ddpInstanceId) {
        SimpleResult results = inTransaction((conn) -> {
            SimpleResult dbVals = new SimpleResult();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_SELECT_RANDOM_PT)) {
                stmt.setString(1, ddpInstanceId);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    dbVals.resultValue = rs.getString(DBConstants.DDP_PARTICIPANT_ID);
                }
            }
            catch (SQLException e) {
                dbVals.resultException = e;
            }
            return dbVals;
        });
        if (results.resultException != null) {
            throw new RuntimeException("Problem getting a random participant id for instance id " + ddpInstanceId, results.resultException);
        }
        if (results.resultValue != null) {
            return (String) results.resultValue;
        }
        return null;
    }

    public static String getCollaboratorSampleId(int kitTypeId, String participantCollaboratorId, String kitType) {
        SimpleResult results = inTransaction((conn) -> {
            SimpleResult dbVals = new SimpleResult();
            String collaboratorSampleId = KitRequestShipping.generateBspSampleID(conn, participantCollaboratorId, kitType, kitTypeId);
            dbVals.resultValue = collaboratorSampleId;
            return dbVals;
        });
        if (results.resultException != null) {
            throw new RuntimeException("Error getting Collaborator Sample Id for  " + participantCollaboratorId, results.resultException);
        }
        else {
            return (String) results.resultValue;
        }
    }

    public Object getBSPKitInfo(@NonNull String kitLabel, @NonNull Response response, NotificationUtil notificationUtil) {
        return TransactionWrapper.inTransaction(conn -> {
            logger.info("Checking label " + kitLabel);
            BSPKitQueryResult bspKitQueryResult = BSPKitQueryResult.getBSPKitQueryResult(kitLabel);

            if (bspKitQueryResult == null) {
                logger.info("No kit w/ label " + kitLabel + " found");
                response.status(404);
                return null;
            }
            boolean firstTimeReceived = KitUtil.setKitReceived(conn, kitLabel);
            if (StringUtils.isNotBlank(bspKitQueryResult.getParticipantExitId())) {
                String message = "Kit of exited participant " + bspKitQueryResult.getBspParticipantId() + " was received by GP.<br>";
                notificationUtil.sentNotification(bspKitQueryResult.getNotificationRecipient(), message, NotificationUtil.DSM_SUBJECT);
                return new BSPKitStatus(BSPKitStatus.EXITED);
            }
            if (StringUtils.isNotBlank(bspKitQueryResult.getDeactivationDate())) {
                return new BSPKitStatus(BSPKitStatus.DEACTIVATED);
            }
            return receiveBSPKit(kitLabel, bspKitQueryResult, notificationUtil, conn, firstTimeReceived);
        });

    }

    private BSPKitInfo receiveBSPKit(String kitLabel, BSPKitQueryResult bspKitQueryResult, NotificationUtil notificationUtil, @NonNull Connection conn, boolean firstTimeReceived) {
        if (StringUtils.isNotBlank(bspKitQueryResult.getDdpParticipantId())) {
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
    }

    private static void writeSampleReceivedToES(DDPInstance ddpInstance, BSPKitQueryResult bspKitInfo) {
        String kitRequestId = new KitRequestDao().getKitRequestIdByBSPParticipantId(bspKitInfo.getBspParticipantId());
        Map<String, Object> nameValuesMap = new HashMap<>();
        nameValuesMap.put(ESObjectConstants.RECEIVED, SystemUtil.getISO8601DateString());
        ElasticSearchUtil.writeSample(ddpInstance, kitRequestId, bspKitInfo.getDdpParticipantId(), ESObjectConstants.SAMPLES,
                ESObjectConstants.KIT_REQUEST_ID, nameValuesMap);
    }

    private static void triggerDDP(Connection conn, @NonNull BSPKitQueryResult bspKitInfo, boolean firstTimeReceived, String kitLabel) {
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

    @Override
    public int create(BSPKitDao bspKitDao) {
        return 0;
    }

    @Override
    public int delete(int id) {
        return 0;
    }

    @Override
    public Optional<BSPKitDao> get(long id) {
        return Optional.empty();
    }
}
