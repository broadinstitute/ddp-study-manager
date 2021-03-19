package org.broadinstitute.dsm.jobs;

import com.google.cloud.functions.BackgroundFunction;
import com.google.cloud.functions.Context;
import com.google.gson.Gson;
import com.typesafe.config.Config;
import org.apache.commons.dbcp2.PoolableConnection;
import org.apache.commons.dbcp2.PoolingDataSource;
import org.apache.commons.lang3.tuple.Pair;
import org.broadinstitute.dsm.careevolve.Authentication;
import org.broadinstitute.dsm.careevolve.Covid19OrderRegistrar;
import org.broadinstitute.dsm.careevolve.Provider;
import org.broadinstitute.dsm.cf.CFUtil;
import org.broadinstitute.dsm.model.KitDDPNotification;
import org.broadinstitute.dsm.model.UPSKit;
import org.broadinstitute.dsm.model.ups.*;
import org.broadinstitute.dsm.shipping.UPSTracker;
import org.broadinstitute.dsm.statics.ApplicationConfigConstants;
import org.broadinstitute.dsm.util.EventUtil;
import org.broadinstitute.dsm.util.KitUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.utils.StringUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;


public class TestBostonUPSTrackingJob implements BackgroundFunction<PubsubMessage> {

    private String STUDY_MANAGER_SCHEMA = System.getenv("STUDY_MANAGER_SCHEMA") + ".";
    private String STUDY_SERVER_SCHEMA = System.getenv("STUDY_SERVER_SCHEMA") + ".";

    private final Logger logger = LoggerFactory.getLogger(TestBostonUPSTrackingJob.class);

    String DELIVERED = "DELIVERED";
    String RECEIVED = "RECEIVED";

    private String SELECT_BY_EXTERNAL_ORDER_NUMBER = "and request.external_order_number = ?";
    private Covid19OrderRegistrar orderRegistrar;
    String endpoint;
    String username;
    String password;
    String accessKey;

    @Override
    public void accept(PubsubMessage message, Context context) throws Exception {
        if (message.data == null) {
            logger.info("No message provided");
            return;
        }
        Config cfg = CFUtil.loadConfig();
        String dbUrl = cfg.getString("dsmDBUrl");
        endpoint = cfg.getString("ups.url");
        username = cfg.getString("ups.username");
        password = cfg.getString("ups.password");
        accessKey = cfg.getString("ups.accesskey");
        PoolingDataSource<PoolableConnection> dataSource = CFUtil.createDataSource(1, dbUrl);
        logger.info("Starting the UPS lookup job");
        String data = new String(Base64.getDecoder().decode(message.data));

        UPSKit[] kitsToLookFor = new Gson().fromJson(data, UPSKit[].class);
        Arrays.stream(kitsToLookFor).forEach(
                kit -> {
                    logger.info("Checking possible actions for kit " + kit.getDsmKitRequestId());
                    if (StringUtils.isNotBlank(kit.getUpsPackage().getUpsShipmentId())) {
                        getUPSUpdate(dataSource, kit, cfg);
                    }
                    else {
                        insertShipmentAndPackageForNewKit(dataSource, kit, cfg);
                    }

                }
        );
    }

    private void insertShipmentAndPackageForNewKit(PoolingDataSource<PoolableConnection> dataSource, UPSKit kit, Config cfg) {
        String insertedShipmentId = null;
        String shippingKitPackageId = null;
        String returnKitPackageId = null;
        final String SQL_INSERT_SHIPMENT = "INSERT INTO " + STUDY_MANAGER_SCHEMA + "ups_shipment" +
                "  ( dsm_kit_request_id )" +
                "  VALUES " +
                "  (?)";
        final String SQL_INSERT_UPSPackage = "INSERT INTO " + STUDY_MANAGER_SCHEMA + "ups_package" +
                "  ( dsm_kit_request_id ," +
                " ups_shipment_id ," +
                " tracking_number ,)" +
                "  VALUES " +
                "  (?, ? ,?)," +
                "  (?, ? ,?) ";
        Connection conn = null;
        try {
            conn = dataSource.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_INSERT_SHIPMENT)) {
                stmt.setString(1, kit.getDsmKitRequestId());
                int result = stmt.executeUpdate();
                if (result == 1) {
                    try (ResultSet rs = stmt.getGeneratedKeys()) {
                        if (rs.next()) {
                            insertedShipmentId = rs.getString(1);
                            logger.info("Added new ups shipment with id " + insertedShipmentId + " for " + kit.getDsmKitRequestId());
                        }
                    }
                    catch (Exception e) {
                        logger.error("Error getting id of new shipment ", e);
                        return;
                    }
                }
                else {
                    logger.error("Error adding new ups shipment for kit w/ id " + kit.getDsmKitRequestId() + " it was updating " + result + " rows");
                    return;
                }

            }
            catch (Exception ex) {
                logger.error("Error getting connection to db ", ex);
                return;
            }
            if (insertedShipmentId != null) {

                try (PreparedStatement stmt = conn.prepareStatement(SQL_INSERT_UPSPackage)) {
                    stmt.setString(1, kit.getDsmKitRequestId());
                    stmt.setString(2, insertedShipmentId);
                    stmt.setString(3, kit.getTrackingToId());//first row is the shipping one
                    stmt.setString(4, kit.getDsmKitRequestId());
                    stmt.setString(5, insertedShipmentId);
                    stmt.setString(6, kit.getTrackingReturnId());//second row is the return one
                    int result = stmt.executeUpdate();
                    if (result == 2) {
                        try (ResultSet rs = stmt.getGeneratedKeys()) {
                            if (rs.next()) {
                                shippingKitPackageId = rs.getString(1);
                                returnKitPackageId = rs.getString(2);
                                logger.info("Added new ups package with id " + shippingKitPackageId + " for " + kit.getDsmKitRequestId());
                                logger.info("Added new ups package with id " + returnKitPackageId + " for " + kit.getDsmKitRequestId());
                            }
                        }
                        catch (Exception e) {
                            logger.error("Error getting id of new packages  ", e);
                            return;
                        }
                    }
                    else {
                        logger.error("Error adding new ups packages for kit w/ id " + kit.getDsmKitRequestId() + " it was updating " + result + " rows");
                        return;
                    }

                }
                catch (Exception ex) {
                    logger.error("Error getting connection to db ", ex);
                    return;
                }
            }
            conn.commit();
            UPSPackage upsPackageShipping = new UPSPackage(kit.getTrackingToId(), null, insertedShipmentId, shippingKitPackageId, kit.getDsmKitRequestId(), null, null);
            UPSPackage upsPackageReturn = new UPSPackage(kit.getTrackingReturnId(), null, insertedShipmentId, returnKitPackageId, kit.getDsmKitRequestId(), null, null);
            UPSKit kitShipping = new UPSKit(upsPackageShipping, kit.getKitLabel(), kit.getCE_order(), kit.getDsmKitRequestId(), kit.getExternalOrderNumber(), kit.getTrackingToId(), kit.getTrackingReturnId(), kit.getDdpInstanceId(), kit.getHruid());
            UPSKit kitReturn = new UPSKit(upsPackageReturn, kit.getKitLabel(), kit.getCE_order(), kit.getDsmKitRequestId(), kit.getExternalOrderNumber(), kit.getTrackingToId(), kit.getTrackingReturnId(), kit.getDdpInstanceId(), kit.getHruid());
            getUPSUpdate(dataSource, kitShipping, cfg);
            getUPSUpdate(dataSource, kitReturn, cfg);
        }
        catch (Exception e) {
            logger.error("unable to connect to DB " + e);
        }
        finally {
            if (conn != null) {
                try {
                    conn.close();
                }
                catch (Throwable ex) {
                    logger.debug("Could not close JDBC Connection on shutdown", ex);
                }
            }
        }
    }

    public void getUPSUpdate(PoolingDataSource<PoolableConnection> dataSource, UPSKit kit, Config cfg) {
        logger.info("Checking UPS status for kit with external order number " + kit.getExternalOrderNumber());
        try {
            updateKitStatus(dataSource, kit, kit.isReturn(), kit.getDdpInstanceId(), cfg);
        }
        catch (Exception e) {
            logger.error("Could not update outbound status for " + kit.getExternalOrderNumber() + " " + e.toString(), e);
        }
    }

    public UPSTrackingResponse lookupTrackingInfo(String trackingId) {
        return new UPSTracker(endpoint, username, password, accessKey).lookupTrackingInfo(trackingId);
    }

    public void updateKitStatus(PoolingDataSource<PoolableConnection> dataSource, UPSKit kit, boolean isReturn, String ddpInstanceId, Config cfg) {
        String trackingId = kit.getUpsPackage().getTrackingNumber();
        UPSActivity lastActivity = kit.getUpsPackage().getActivity() == null ? null : kit.getUpsPackage().getActivity()[0];
        if (lastActivity != null && lastActivity.getStatus().isDelivery()) {
            this.logger.info("Tracking id " + trackingId + " is already delivered, not going to check UPS anymore");
            return;
        }
        logger.info("Checking UPS status for " + trackingId + " for kit w/ external order number " + kit.getExternalOrderNumber());
        UPSTrackingResponse response = lookupTrackingInfo(trackingId);
        logger.info("UPS response for " + trackingId + " is " + response);//todo remove after tests
        if (response != null && response.getErrors() == null) {
            updateStatus(dataSource, trackingId, lastActivity, response, isReturn, kit, ddpInstanceId, cfg);
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


    private void updateStatus(PoolingDataSource<PoolableConnection> dataSource, String trackingId, UPSActivity lastActivity, UPSTrackingResponse
            response, boolean isReturn, UPSKit kit, String ddpInstanceId, Config cfg) {
        String query = "";
        if (response.getTrackResponse() != null) {
            UPSShipment[] shipment = response.getTrackResponse().getShipment();
            if (shipment != null && shipment.length > 0) {
                UPSPackage[] responseUpsPackages = shipment[0].getUpsPackageArray();
                if (responseUpsPackages != null && responseUpsPackages.length > 0) {
                    UPSPackage responseUpsPackage = responseUpsPackages[0];
                    UPSActivity[] activities = responseUpsPackage.getActivity();
                    if (activities != null) {
                        UPSActivity earliestPackageMovementEvent = responseUpsPackage.getEarliestPackageMovementEvent();
                        Instant earliestPackageMovement = null;
                        if (earliestPackageMovementEvent != null) {
                            earliestPackageMovement = earliestPackageMovementEvent.getInstant();
                        }
                        UPSActivity recentActivity = activities[0];
                        UPSStatus status = activities[0].getStatus();
                        String statusType = null;
                        if (status != null) {
                            statusType = status.getType();
                        }
                        if (lastActivity == null || (!lastActivity.equals(recentActivity))) {
                            updateTrackingInfo(dataSource, activities, statusType, lastActivity, trackingId,
                                    isReturn, kit, earliestPackageMovement, ddpInstanceId, cfg);
                        }
                        if (responseUpsPackage.getDeliveryDate() != null) {
                            updateDeliveryInformation(dataSource, cfg, responseUpsPackage);
                        }
                    }
                }

            }
        }
    }

    private void updateDeliveryInformation(PoolingDataSource<PoolableConnection> dataSource, Config cfg, UPSPackage responseUpsPackage) {
        String SQL_UPDATE_PACKAGE_DELIVERY = "UPDATE " + STUDY_MANAGER_SCHEMA + "ups_package   " +
                "SET   " +
                "delivery_date = ?,   " +
                "delivery_time_start = ?,   " +
                "delivery_time_end = ?,   " +
                "delivery_time_type = ?,   " +
                "WHERE ups_package_id = ? ";

        Connection conn = null;
        try {
            conn = dataSource.getConnection();

            try (PreparedStatement stmt = conn.prepareStatement(SQL_UPDATE_PACKAGE_DELIVERY)) {
                stmt.setString(1, responseUpsPackage.getDeliveryDate().getDate());
                stmt.setString(1, responseUpsPackage.getDeliveryTime().getStartTime());
                stmt.setString(1, responseUpsPackage.getDeliveryTime().getEndTime());
                stmt.setString(1, responseUpsPackage.getDeliveryTime().getType());
                stmt.setString(1, responseUpsPackage.getUpsPackageId());
                int r = stmt.executeUpdate();

                logger.info("Updated " + r + " rows adding delivery for " + responseUpsPackage.getUpsPackageId());
                if (r != 1) {
                    logger.error(r + " rows updated in UPSPackege while updating delivery for " + responseUpsPackage.getUpsPackageId());
                }
                conn.commit();
            }
            catch (Exception ex) {
                logger.error("Error getting connection to db ", ex);
            }
        }
        catch (Exception e) {
            throw new RuntimeException("unable to connect to DB " + e);
        }
        finally {
            if (conn != null) {
                try {
                    conn.close();
                }
                catch (Throwable ex) {
                    logger.debug("Could not close JDBC Connection on shutdown", ex);
                }
            }
        }
    }


    private void updateTrackingInfo(PoolingDataSource<PoolableConnection> dataSource,
                                    UPSActivity[] activities,
                                    String statusType,
                                    UPSActivity lastActivity,
                                    String trackingId,
                                    boolean isReturn,
                                    UPSKit kit,
                                    Instant earliestInTransitTime,
                                    String ddpInstanceId,
                                    Config cfg) {
        final String INSERT_NEW_ACTIVITIES = "INSERT INTO " + STUDY_MANAGER_SCHEMA + ".ups_activity    " +
                "(    " +
                "  ups_package_id  ,  " +
                "  dsm_kit_request_id  ,  " +
                "  ups_location  ,  " +
                "  ups_status_type  ,  " +
                "  ups_status_description  ,  " +
                "  ups_status_code  ,  " +
                "  ups_activity_date  ,  " +
                "  ups_activity_time  )  " +
                "VALUES " +
                "(?, ?, ?, ?, ?, ?, ?, ?)";
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
        Connection conn = null;
        try {
            conn = dataSource.getConnection();
            int i = 0;
            for (UPSActivity activity : activities) {
                if (activity.getInstant().isBefore(lastActivity.getInstant())) {
                    i++;
                }
            }
            for (; i < activities.length; i++) {
                UPSActivity currentInsertingActivity = activities[i];
                try (PreparedStatement stmt = conn.prepareStatement(INSERT_NEW_ACTIVITIES)) {
                    stmt.setString(1, kit.getUpsPackage().getUpsPackageId());
                    stmt.setString(1, kit.getDsmKitRequestId());
                    stmt.setString(1, currentInsertingActivity.getLocation().getString());
                    stmt.setString(1, currentInsertingActivity.getStatus().getType());
                    stmt.setString(1, currentInsertingActivity.getStatus().getDescription());
                    stmt.setString(1, currentInsertingActivity.getStatus().getCode());
                    stmt.setString(1, currentInsertingActivity.getDate());
                    stmt.setString(1, currentInsertingActivity.getTime());
                    int r = stmt.executeUpdate();

                    logger.info("Updated " + r + " rows for a new activity");
                    if (r != 1) {
                        logger.error(r + " is too big for 1 new activity");
                    }
                    String oldType = null;
                    if (lastActivity != null && lastActivity.getStatus() != null) {
                        oldType = lastActivity.getStatus().getType();
                    }
                    if (!isReturn) {
                        if (shouldTriggerEventForKitOnItsWayToParticipant(statusType, oldType)) {
                            KitDDPNotification kitDDPNotification = KitDDPNotification.getKitDDPNotification(conn, SQL_SELECT_KIT_FOR_NOTIFICATION_EXTERNAL_SHIPPER + SELECT_BY_EXTERNAL_ORDER_NUMBER, new String[] { DELIVERED, ddpInstanceId, kit.getDsmKitRequestId(), kit.getExternalOrderNumber() }, 1);
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
                        if (earliestInTransitTime != null && !kit.getCE_order()) {
                            // if we have the first date of an inbound event, create an order in CE
                            // using the earliest date of inbound event
                            Authentication careEvolveAuth = null;
                            if (orderRegistrar == null) {
                                Pair<Covid19OrderRegistrar, Authentication> careEvolveOrderingTools = createCEOrderRegistrar(cfg);
                                orderRegistrar = careEvolveOrderingTools.getLeft();
                                careEvolveAuth = careEvolveOrderingTools.getRight();

                            }
                            orderRegistrar.orderTest(careEvolveAuth, kit.getHruid(), kit.getMainKitLabel(), kit.getExternalOrderNumber(), earliestInTransitTime);
                            logger.info("Placed CE order for kit with external order number " + kit.getExternalOrderNumber());
                            kit.changeCEOrdered(conn, true);
                        }
                        else {
                            logger.info("No return events for " + kit.getMainKitLabel() + ".  Will not place order yet.");
                        }
                        if (shouldTriggerEventForReturnKitDelivery(statusType, oldType)) {
                            KitUtil.setKitReceived(conn, kit.getMainKitLabel());
                            logger.info("RECEIVED: " + trackingId);
                            KitDDPNotification kitDDPNotification = KitDDPNotification.getKitDDPNotification(conn, SQL_SELECT_KIT_FOR_NOTIFICATION_EXTERNAL_SHIPPER + SELECT_BY_EXTERNAL_ORDER_NUMBER, new String[] { RECEIVED, ddpInstanceId, kit.getDsmKitRequestId(), kit.getExternalOrderNumber() }, 1);
                            if (kitDDPNotification != null) {
                                logger.info("Triggering DDP for received kit with external order number: " + kit.getExternalOrderNumber());
                                EventUtil.triggerDDP(conn, kitDDPNotification);

                            }
                            else {
                                logger.error("received kitDDPNotification was null for " + kit.getExternalOrderNumber());
                            }
                        }
                    }
                    conn.commit();
                    logger.info("Updated status of tracking number " + trackingId + " to " + statusType + " from " + oldType + " for kit w/ external order number " + kit.getExternalOrderNumber());
                }
                catch (Exception ex) {
                    logger.error("Error getting connection to db ", ex);
                }

            }
        }
        catch (Exception e) {
            throw new RuntimeException("unable to connect to DB " + e);
        }
        finally {
            if (conn != null) {
                try {
                    conn.close();
                }
                catch (Throwable ex) {
                    logger.debug("Could not close JDBC Connection on shutdown", ex);
                }
            }
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


    private Pair<Covid19OrderRegistrar, Authentication> createCEOrderRegistrar(Config cfg) {
        Covid19OrderRegistrar orderRegistrar;
        String careEvolveSubscriberKey = cfg.getString(ApplicationConfigConstants.CARE_EVOLVE_SUBSCRIBER_KEY);
        String careEvolveServiceKey = cfg.getString(ApplicationConfigConstants.CARE_EVOLVE_SERVICE_KEY);
        Authentication careEvolveAuth = new Authentication(careEvolveSubscriberKey, careEvolveServiceKey);
        String careEvolveAccount = cfg.getString(ApplicationConfigConstants.CARE_EVOLVE_ACCOUNT);
        String careEvolveOrderEndpoint = cfg.getString(ApplicationConfigConstants.CARE_EVOLVE_ORDER_ENDPOINT);
        Integer careEvolveMaxRetries;
        Integer careEvolveRetyWaitSeconds;
        if (cfg.hasPath(ApplicationConfigConstants.CARE_EVOLVE_MAX_RETRIES)) {
            careEvolveMaxRetries = cfg.getInt(ApplicationConfigConstants.CARE_EVOLVE_MAX_RETRIES);
        }
        else {
            careEvolveMaxRetries = 5;
        }
        if (cfg.hasPath(ApplicationConfigConstants.CARE_EVOLVE_RETRY_WAIT_SECONDS)) {
            careEvolveRetyWaitSeconds = cfg.getInt(ApplicationConfigConstants.CARE_EVOLVE_RETRY_WAIT_SECONDS);
        }
        else {
            careEvolveRetyWaitSeconds = 10;
        }
        logger.info("Will retry CareEvolve at most {} times after {} seconds", careEvolveMaxRetries, careEvolveRetyWaitSeconds);
        Provider provider;
        provider = new Provider(cfg.getString(ApplicationConfigConstants.CARE_EVOLVE_PROVIDER_FIRSTNAME),
                cfg.getString(ApplicationConfigConstants.CARE_EVOLVE_PROVIDER_LAST_NAME),
                cfg.getString(ApplicationConfigConstants.CARE_EVOLVE_PROVIDER_NPI));
        orderRegistrar = new Covid19OrderRegistrar(careEvolveOrderEndpoint, careEvolveAccount, provider,
                careEvolveMaxRetries, careEvolveRetyWaitSeconds);
        Pair result = Pair.of(orderRegistrar, careEvolveAuth);
        return result;
    }

}




