package org.broadinstitute.dsm.jobs;

import com.google.cloud.functions.BackgroundFunction;
import com.google.cloud.functions.Context;
import com.google.gson.Gson;
import com.google.pubsub.v1.PubsubMessage;
import com.typesafe.config.Config;
import lombok.NonNull;
import org.apache.commons.dbcp2.PoolableConnection;
import org.apache.commons.dbcp2.PoolingDataSource;
import org.broadinstitute.dsm.DSMServer;
import org.broadinstitute.dsm.careevolve.Covid19OrderRegistrar;
import org.broadinstitute.dsm.cf.CFUtil;
import org.broadinstitute.dsm.db.DDPInstance;
import org.broadinstitute.dsm.db.DdpKit;
import org.broadinstitute.dsm.model.KitDDPNotification;
import org.broadinstitute.dsm.model.ups.*;
import org.broadinstitute.dsm.shipping.UPSTracker;
import org.broadinstitute.dsm.statics.DBConstants;
import org.broadinstitute.dsm.util.EventUtil;
import org.broadinstitute.dsm.util.KitUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.utils.StringUtils;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class TestBostonUPSTrackingJob implements BackgroundFunction<PubsubMessage> {

    private String STUDY_MANAGER_SCHEMA = System.getenv("STUDY_MANAGER_SCHEMA") + ".";
    private String STUDY_SERVER_SCHEMA = System.getenv("STUDY_SERVER_SCHEMA") + ".";

    private final Logger logger = LoggerFactory.getLogger(TestBostonUPSTrackingJob.class);

    private String SQL_AVOID_DELIVERED = "and (tracking_to_id is not null or tracking_return_id is not null ) and (kit_shipping_history is null or kit_shipping_history not like \"" + UPSStatus.DELIVERED_TYPE + " %\" or kit_return_history is null or kit_return_history not like \"" + UPSStatus.DELIVERED_TYPE + " %\")" +
            " order by kit.dsm_kit_request_id ASC";


    String DELIVERED = "DELIVERED";
    String RECEIVED = "RECEIVED";

    private String SELECT_BY_EXTERNAL_ORDER_NUMBER = "and request.external_order_number = ?";
//    private Covid19OrderRegistrar orderRegistrar;

    @Override
    public void accept(PubsubMessage pubsubMessage, Context context) throws Exception {
        Config cfg = CFUtil.loadConfig();
        String dbUrl = cfg.getString("dsmDBUrl");

        PoolingDataSource<PoolableConnection> dataSource = CFUtil.createDataSource(1, dbUrl);

        // static vars are dangerous in CF https://cloud.google.com/functions/docs/bestpractices/tips#functions-tips-scopes-java
        final String SQL_SELECT_KITS = "SELECT kit.dsm_kit_request_id, kit.kit_label, kit.tracking_to_id, kit.tracking_return_id, kit.error, kit.message, kit.receive_date, kit.kit_shipping_history,  kit.kit_return_history,  req.bsp_collaborator_participant_id, " +
                " req.external_order_number, kit.CE_order FROM " + STUDY_MANAGER_SCHEMA + "ddp_kit kit LEFT JOIN " + STUDY_MANAGER_SCHEMA + "ddp_kit_request req " +
                " ON (kit.dsm_kit_request_id = req.dsm_kit_request_id) WHERE req.ddp_instance_id = ? and kit_label not like \"%\\\\_1\"  ";

        logger.info("Starting the UPS lookup job");

        // static vars are dangerous in CF https://cloud.google.com/functions/docs/bestpractices/tips#functions-tips-scopes-java

//        orderRegistrar = new Covid19OrderRegistrar(DSMServer.careEvolveOrderEndpoint, DSMServer.careEvolveAccount, DSMServer.provider,
//                DSMServer.careEvolveMaxRetries, DSMServer.careEvolveRetyWaitSeconds);
        try (Connection conn = dataSource.getConnection()) {
            List<DDPInstance> ddpInstanceList = getDDPInstanceListWithRole(conn, "ups_tracking");
            for (DDPInstance ddpInstance : ddpInstanceList) {
                if (ddpInstance != null && ddpInstance.isHasRole()) {
                    logger.info("tracking ups ids for " + ddpInstance.getName());
                    String query = SQL_SELECT_KITS + SQL_AVOID_DELIVERED;
                    try (PreparedStatement stmt = conn.prepareStatement(query)) {
                        stmt.setString(1, ddpInstance.getDdpInstanceId());
                        logger.info("Got results for " + ddpInstance.getName());
                        try (ResultSet rs = stmt.executeQuery()) {
                            while (rs.next()) {
                                DdpKit kit = new DdpKit(
                                        rs.getString(DBConstants.DSM_KIT_REQUEST_ID),
                                        rs.getString("kit." + DBConstants.KIT_LABEL),
                                        rs.getString("kit." + DBConstants.DSM_TRACKING_TO),
                                        rs.getString("kit." + DBConstants.TRACKING_RETURN_ID),
                                        rs.getString("kit." + DBConstants.ERROR),
                                        rs.getString("kit." + DBConstants.MESSAGE),
                                        rs.getString("kit." + DBConstants.DSM_RECEIVE_DATE),
                                        rs.getString("req." + DBConstants.COLLABORATOR_PARTICIPANT_ID),
                                        rs.getString("req." + DBConstants.EXTERNAL_ORDER_NUMBER),
                                        rs.getBoolean("kit." + DBConstants.CE_ORDER),
                                        rs.getString("kit." + DBConstants.KIT_SHIPPING_HISTORY),
                                        rs.getString("kit." + DBConstants.KIT_RETURN_HISTORY)
                                );
                                logger.info(kit.getExternalOrderNumber());

                                if (StringUtils.isNotBlank(kit.getTrackingToId()) && !kit.isDelivered()) {
                                    try {
                                        updateKitStatus(conn, kit, false, ddpInstance, cfg);
                                    }
                                    catch (Exception e) {
                                        logger.error("Could not update outbound status for " + kit.getExternalOrderNumber() + " " + e.toString(), e);
                                    }
                                }

                                if (StringUtils.isNotBlank(kit.getTrackingReturnId()) && !kit.isReturned()) {
                                    try {
                                        updateKitStatus(conn, kit, true, ddpInstance, cfg);
                                    }
                                    catch (Exception e) {
                                        logger.error("Could not update return status for " + kit.getExternalOrderNumber() + " " + e.toString(), e);
                                    }
                                }
                            }
                        }
                    }
                    catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }

    public UPSTrackingResponse lookupTrackingInfo(Config cfg, String trackingId) throws IOException {
        logger.info(DSMServer.UPS_ENDPOINT);
        String endpoint = cfg.getString("ups.url");
        String username = cfg.getString("ups.username");
        String password = cfg.getString("ups.password");
        String accessKey = cfg.getString("ups.accesskey");
        logger.info(DSMServer.UPS_ENDPOINT);
        logger.info(endpoint);
        return new UPSTracker(endpoint, username, password, accessKey).lookupTrackingInfo(trackingId);
    }

    public void updateKitStatus(@NonNull Connection conn, DdpKit kit, boolean isReturn, DDPInstance ddpInstance, Config cfg) throws IOException {
        String trackingId;
        if (!isReturn) {
            trackingId = kit.getTrackingToId();
        }
        else {
            trackingId = kit.getTrackingReturnId();
        }
        String type = null;
        UPSActivity lastActivity = null;
        if (isReturn) {
            if (kit.getKitReturnHistory() != null) {
                lastActivity = new Gson().fromJson(kit.getKitReturnHistory(), UPSActivity[].class)[0];
            }
        }
        else {
            if (kit.getKitShippingHistory() != null) {
                lastActivity = new Gson().fromJson(kit.getKitShippingHistory(), UPSActivity[].class)[0];
            }
        }
        if (lastActivity != null && lastActivity.getStatus().isDelivery()) {
            this.logger.info("Tracking id " + trackingId + " is already delivered, not going to check UPS anymore");
            return;
        }

        logger.info("Checking UPS status for " + trackingId + " for kit w/ external order number " + kit.getExternalOrderNumber());
        UPSTrackingResponse response = lookupTrackingInfo(cfg, trackingId);
        logger.info("UPS response for " + trackingId + " is " + response);

        if (response != null && response.getErrors() == null) {
            updateStatus(conn, trackingId, lastActivity, response, isReturn, kit, ddpInstance);
        }
        else {
            logError(trackingId, response.getErrors());
        }
    }

    private void logError(String trackingId, UPSError[] errors) {
        String errorString = "";
        for (UPSError error : errors) {
            errorString += "Got Error: " + error.getCode() + " " + error.getMessage() + " For Tracking Number " + trackingId;
        }
        logger.error(errorString);
    }

    private void updateStatus(@NonNull Connection conn, String trackingId, UPSActivity lastActivity, UPSTrackingResponse response, boolean isReturn, DdpKit kit, DDPInstance ddpInstance) {
        final String SQL_UPDATE_UPS_TRACKING_STATUS = "UPDATE " + STUDY_MANAGER_SCHEMA + "ddp_kit SET kit_shipping_history = ? " +
                "WHERE dsm_kit_id <> 0 and  tracking_to_id = ? and dsm_kit_request_id in ( SELECT dsm_kit_request_id FROM " + STUDY_MANAGER_SCHEMA + "ddp_kit_request where external_order_number = ? )";

        final String SQL_UPDATE_UPS_RETURN_STATUS = "UPDATE " + STUDY_MANAGER_SCHEMA + "ddp_kit SET kit_return_history = ? " +
                "WHERE dsm_kit_id <> 0 and tracking_return_id= ? and dsm_kit_request_id in ( SELECT dsm_kit_request_id FROM " + STUDY_MANAGER_SCHEMA + "ddp_kit_request where external_order_number = ? )";
//        final String SQL_


        if (response.getTrackResponse() != null) {
            UPSShipment[] shipment = response.getTrackResponse().getShipment();

            if (shipment != null && shipment.length > 0) {
                UPSPackage[] upsPackages = shipment[0].getUpsPackageArray();
                if (upsPackages != null && upsPackages.length > 0) {
                    UPSPackage upsPackage = upsPackages[0];
                    UPSActivity[] activity = upsPackage.getActivity();
                    String upsHistory = new Gson().toJson(activity);

                    if (activity != null) {
                        String sqlUpdate = SQL_UPDATE_UPS_TRACKING_STATUS;

                        UPSActivity earliestPackageMovementEvent = upsPackage.getEarliestPackageMovementEvent();
                        Instant earliestPackageMovement = null;
                        if (earliestPackageMovementEvent != null) {
                            earliestPackageMovement = earliestPackageMovementEvent.getInstant();
                        }
                        if (isReturn) {
                            sqlUpdate = SQL_UPDATE_UPS_RETURN_STATUS;
                        }
                        UPSActivity recentActivity = activity[0];
                        UPSStatus status = activity[0].getStatus();
                        String statusType = null;
                        if (status != null) {
                            statusType = status.getType();
                        }
                        if (lastActivity == null || (!lastActivity.equals(recentActivity))) {
                            updateTrackingInfo(conn, upsHistory, activity, statusType, lastActivity, trackingId, sqlUpdate,
                                    isReturn, kit, earliestPackageMovement, ddpInstance);
                        }
                    }
                }
            }
        }
    }


    private void updateTrackingInfo(@NonNull Connection conn,
                                    String upsHistory,
                                    UPSActivity[] upsActivities,
                                    String statusType,
                                    UPSActivity lastActivity,
                                    String trackingId,
                                    String query,
                                    boolean isReturn,
                                    DdpKit kit,
                                    Instant earliestInTransitTime,
                                    DDPInstance ddpInstance) {
        final String SQL_SELECT_KIT_FOR_NOTIFICATION_EXTERNAL_SHIPPER = "select  eve.*,   request.ddp_participant_id,   request.ddp_label,   request.dsm_kit_request_id, request.ddp_kit_request_id, request.upload_reason, " +
                "        realm.ddp_instance_id, realm.instance_name, realm.base_url, realm.auth0_token, realm.notification_recipients, realm.migrated_ddp, kit.receive_date, kit.scan_date" +
                "        FROM " + STUDY_MANAGER_SCHEMA + "ddp_kit_request request, " + STUDY_MANAGER_SCHEMA + "ddp_kit kit, " + STUDY_MANAGER_SCHEMA + "event_type eve, " + STUDY_MANAGER_SCHEMA + "ddp_instance realm where request.dsm_kit_request_id = kit.dsm_kit_request_id and request.ddp_instance_id = realm.ddp_instance_id" +
                "        and not exists " +
                "                    (select 1 FROM " + STUDY_MANAGER_SCHEMA + "EVENT_QUEUE q" +
                "                    where q.DDP_INSTANCE_ID = realm.ddp_instance_id" +
                "                    and " +
                "                    q.EVENT_TYPE = eve.event_name" +
                "                    and " +
                "                    q.DSM_KIT_REQUEST_ID = request.dsm_kit_request_id " +
                "                    and q.event_triggered = true" +
                "                    )" +
                "        and (eve.ddp_instance_id = request.ddp_instance_id and eve.kit_type_id = request.kit_type_id) and eve.event_type = ? " +
                "         and realm.ddp_instance_id = ?" +
                "          and kit.dsm_kit_request_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, upsHistory);
            stmt.setString(2, trackingId);
            stmt.setString(3, kit.getExternalOrderNumber());
//            logger.info(stmt.toString());
            int r = stmt.executeUpdate();
            if (r != 2) {//number of subkits
                logger.error("Update query for UPS tracking updated " + r + " rows! with tracking/return id: " + trackingId + " for kit w/ external order number " + kit.getExternalOrderNumber());
            }
            String oldType = null;
            if (lastActivity != null && lastActivity.getStatus() != null) {
                oldType = lastActivity.getStatus().getType();
            }
            if (!isReturn) {
                if (shouldTriggerEventForKitOnItsWayToParticipant(statusType, oldType)) {
                    KitDDPNotification kitDDPNotification = KitDDPNotification.getKitDDPNotification(conn, SQL_SELECT_KIT_FOR_NOTIFICATION_EXTERNAL_SHIPPER + SELECT_BY_EXTERNAL_ORDER_NUMBER, new String[] { DELIVERED, ddpInstance.getDdpInstanceId(), kit.getDsmKitRequestId(), kit.getExternalOrderNumber() }, 2);//todo change this to the number of subkits but for now 2 for test boston works
                    if (kitDDPNotification != null) {
                        logger.info("Triggering DDP for kit going to participant with external order number: " + kit.getExternalOrderNumber());
                        EventUtil.triggerDDP(conn, kitDDPNotification);
                    }
                    else {
                        logger.error("delivered kitDDPNotification was null for " + kit.getExternalOrderNumber());
                    }
                }

            }
            else { // this is a return
                if (earliestInTransitTime != null && !kit.isCEOrdered()) {
                    // if we have the first date of an inbound event, create an order in CE
                    // using the earliest date of inbound event
//                    orderRegistrar.orderTest(DSMServer.careEvolveAuth, kit.getHRUID(), kit.getMainKitLabel(), kit.getExternalOrderNumber(), earliestInTransitTime);
                    logger.info("Placed CE order for kit with external order number " + kit.getExternalOrderNumber());
                    kit.changeCEOrdered(conn, true);
                }
                else {
                    logger.info("No return events for " + kit.getMainKitLabel() + ".  Will not place order yet.");
                }
                if (shouldTriggerEventForReturnKitDelivery(statusType, oldType)) {
                    KitUtil.setKitReceived(conn, kit.getMainKitLabel());
                    logger.info("RECEIVED: " + trackingId);
                    KitDDPNotification kitDDPNotification = KitDDPNotification.getKitDDPNotification(conn, SQL_SELECT_KIT_FOR_NOTIFICATION_EXTERNAL_SHIPPER + SELECT_BY_EXTERNAL_ORDER_NUMBER, new String[] { RECEIVED, ddpInstance.getDdpInstanceId(), kit.getDsmKitRequestId(), kit.getExternalOrderNumber() }, 2);//todo change this to the number of subkits but for now 2 for test boston works
                    if (kitDDPNotification != null) {
                        logger.info("Triggering DDP for received kit with external order number: " + kit.getExternalOrderNumber());
                        EventUtil.triggerDDP(conn, kitDDPNotification);

                    }
                    else {
                        logger.error("received kitDDPNotification was null for " + kit.getExternalOrderNumber());
                    }
                }
            }
                logger.info("Updated status of tracking number " + trackingId + " to " + statusType + " from " + oldType + " for kit w/ external order number " + kit.getExternalOrderNumber());
        }
        catch (Exception e) {
            throw new RuntimeException("Could not update tracking info for tracking id " + trackingId, e);
        }
    }

    /**
     * Determines whether or not a trigger should be sent to
     * study-server to respond to kit being sent to participant
     */
    private boolean shouldTriggerEventForKitOnItsWayToParticipant(String currentStatus, String previousStatus) {
        List<String> triggerStates = Arrays.asList(UPSStatus.DELIVERED_TYPE, UPSStatus.IN_TRANSIT_TYPE);
        return triggerStates.contains(currentStatus) && !triggerStates.contains(previousStatus);
    }

    /**
     * Determines whether or not a trigger should be sent to
     * study-server to respond to kit being delivered back at broad
     */
    private boolean shouldTriggerEventForReturnKitDelivery(String currentStatus, String previousStatus) {
        List<String> triggerStates = Arrays.asList(UPSStatus.DELIVERED_TYPE);
        return triggerStates.contains(currentStatus) && !triggerStates.contains(previousStatus);
    }

    public List<DDPInstance> getDDPInstanceListWithRole(Connection conn, @NonNull String role) {
        final String SQL_SELECT_INSTANCE_WITH_ROLE = "SELECT ddp_instance_id, instance_name, base_url, collaborator_id_prefix, migrated_ddp, billing_reference, " +
                "es_participant_index, es_activity_definition_index,  es_users_index,  carrier_username, carrier_password, carrier_accesskey, carrier_tracking_url, (SELECT count(role.name) " +
                "FROM " + STUDY_MANAGER_SCHEMA + "ddp_instance realm, " + STUDY_MANAGER_SCHEMA + "ddp_instance_role inRol, " + STUDY_MANAGER_SCHEMA + "instance_role role WHERE realm.ddp_instance_id = inRol.ddp_instance_id AND inRol.instance_role_id = role.instance_role_id AND role.name = ? " +
                "AND realm.ddp_instance_id = main.ddp_instance_id) AS 'has_role', mr_attention_flag_d, tissue_attention_flag_d, auth0_token, notification_recipients FROM  " + STUDY_MANAGER_SCHEMA + "ddp_instance main " +
                "WHERE is_active = 1";

        List<DDPInstance> ddpInstances = new ArrayList<>();
        try (PreparedStatement bspStatement = conn.prepareStatement(SQL_SELECT_INSTANCE_WITH_ROLE)) {
            bspStatement.setString(1, role);
            try (ResultSet rs = bspStatement.executeQuery()) {
                while (rs.next()) {
                    DDPInstance ddpInstance = DDPInstance.getDDPInstanceWithRoleFormResultSet(rs);
                    ddpInstances.add(ddpInstance);
                }
            }
        }
        catch (SQLException ex) {
            throw new RuntimeException("Error looking ddpInstances ", ex);
        }

        return ddpInstances;
    }
}
