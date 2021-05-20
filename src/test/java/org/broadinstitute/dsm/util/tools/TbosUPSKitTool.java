package org.broadinstitute.dsm.util.tools;

import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.ddp.db.SimpleResult;
import org.broadinstitute.dsm.db.DDPInstance;
import org.broadinstitute.dsm.statics.DBConstants;
import org.broadinstitute.dsm.util.model.Kit;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import static org.broadinstitute.ddp.db.TransactionWrapper.inTransaction;

public class TbosUPSKitTool {
    public static final Logger logger = LoggerFactory.getLogger(TbosUPSKitTool.class);
    public static final String SQL_SELECT_KIT_BY_KIT_LABEL = "Select kit.dsm_kit_request_id, kit.kit_label, kit.test_result, req.upload_reason, req.ddp_participant_id, req.bsp_collaborator_participant_id, req.created_date, kit.tracking_to_id, kit.tracking_return_id, activity.*,pack.ups_shipment_id, pack.tracking_number " +
            "FROM           ddp_kit kit      " +
            "          LEFT JOIN              ddp_kit_request req  ON (kit.dsm_kit_request_id = req.dsm_kit_request_id)       " +
            "          left join             ups_shipment shipment on (shipment.dsm_kit_request_id = kit.dsm_kit_request_id)     " +
            "          left join           ups_package pack on ( pack.ups_shipment_id = shipment.ups_shipment_id)     " +
            "          left join           ups_activity activity on (pack.ups_package_id = activity.ups_package_id)     " +
            "          WHERE req.ddp_instance_id = ? and ( kit_label not like  \"%\\\\_1\" ) and kit.dsm_kit_request_id > 0     " +
            "           and (shipment.ups_shipment_id is null or activity.ups_activity_id is null  or  activity.ups_activity_id in      " +
            "          ( SELECT ac.ups_activity_id      " +
            "          FROM ups_package pac INNER JOIN      " +
            "             ( SELECT  ups_package_id, MAX(ups_activity_id) maxId      " +
            "             FROM ups_activity      " +
            "             GROUP BY ups_package_id  ) lastActivity ON pac.ups_package_id = lastActivity.ups_package_id INNER JOIN      " +
            "             ups_activity ac ON   lastActivity.ups_package_id = ac.ups_package_id      " +
            "             AND lastActivity.maxId = ac.ups_activity_id      " +
            "          ))  " +
            "       and kit_label=?  ";
    public static final String SQL_SELECT_OUTBOUND = "and (pack.tracking_number is null or pack.tracking_number = kit.tracking_to_id) ";
    public static final String SQL_SELECT_INBOUND = "and (pack.tracking_number is null or pack.tracking_number = kit.tracking_return_id) ";
    private static final String SQL_INSERT_SHIPMENT = "INSERT INTO ups_shipment ( dsm_kit_request_id ) VALUES (?) ";
    private static final String SQL_INSERT_PACKAGE = "INSERT INTO ups_package ( ups_shipment_id , tracking_number ) VALUES (?, ?) ";
    private static final String SQL_INSERT_ACTIVITY = "INSERT INTO ups_activity ( ups_package_id, ups_status_description, ups_activity_date_time) VALUES (?, ?, ?)";

    public static Map readFile(String fileName) {
        Map<String, ArrayList<Kit>> participants = new HashMap();
        String[] headers = new String[0]; //first row
        File csv = new File(fileName);
        try {
            Scanner scanner = new Scanner(csv);
            String headerString = scanner.nextLine();
            headers = headerString.split(",");
            Kit kit = null;
            String shortId = null;
            while (scanner.hasNextLine()) {
                if (kit != null && !kit.isEmpty() && StringUtils.isNotBlank(shortId)) {// last kit of the participant in the line before
                    if (!participants.containsKey(shortId)) {
                        participants.put(shortId, new ArrayList<Kit>());
                    }
                    ArrayList<Kit> list = participants.get(shortId);
                    list.add(kit);
                    participants.put(shortId, list);
                    kit = null;
                }
                String participantLine = scanner.nextLine();
                String[] participantInfo = participantLine.split(",", -1);
                String guid = participantInfo[0]; //read first cell
                shortId = participantInfo[1]; //read second cell
                participants.put(shortId, new ArrayList<Kit>());
                int i = 2;
                for (; i < headers.length; ) {
                    String header = headers[i];
                    String value = participantInfo[i];
                    if (StringUtils.isBlank(value) || "-".equals(value)) {
                        value = null;
                    }
                    if ("kit label".equals(header)) {
                        if (kit != null && !kit.isEmpty() && StringUtils.isNotBlank(kit.getKitLabel())) {
                            if (!participants.containsKey(shortId)) {
                                participants.put(shortId, new ArrayList<Kit>());
                            }
                            ArrayList<Kit> list = participants.get(shortId);
                            list.add(kit);
                            participants.put(shortId, list);
                            kit = null;
                        }
                        else if (kit != null && StringUtils.isBlank(kit.getKitLabel())) {
                            kit = null;
                        }
                    }
                    if (kit == null) {
                        kit = new Kit();
                        kit.setShortId(shortId);
                        kit.setGuid(guid);
                    }
                    kit.setValueByHeader(header, value);
                    i++;
                }
            }
            if (kit != null && !kit.isEmpty() && StringUtils.isNotBlank(shortId)) {// last kit of the participant in the last line
                if (!participants.containsKey(shortId)) {
                    participants.put(shortId, new ArrayList<Kit>());
                }
                ArrayList<Kit> list = participants.get(shortId);
                list.add(kit);
                participants.put(shortId, list);
                kit = null;
            }

        }
        catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        return participants;
    }

    private static void insertFileInfo(Map<String, ArrayList<Kit>> participants) {
        DDPInstance ddpInstance = DDPInstance.getDDPInstance("testboston");
        String[] shortIds = participants.keySet().toArray(new String[0]);
        for (String participantShortId : shortIds) {
            ArrayList<Kit> kits = participants.get(participantShortId);
            for (Kit kit : kits) {
                Kit outboundKit = getDDBKitBasedOnKitLabel(SQL_SELECT_KIT_BY_KIT_LABEL + SQL_SELECT_OUTBOUND, kit.getKitLabel(), ddpInstance.getDdpInstanceId());
                if (outboundKit == null) {
                    logger.error("Outbound Kit found for " + kit.getKitLabel() + " is null!");
                }
                else {
                    if (kit.getGuid() != outboundKit.getGuid() || kit.getShortId() != outboundKit.getShortId()) {
                        throw new RuntimeException("2 kits don't match for outbound for kitLabel " + kit.getKitLabel() + " file kit is for participant (" + kit.getShortId() + "," + kit.getGuid() + ") and DB kit is for participant (" + outboundKit.getShortId() + "," + outboundKit.getGuid() + ")");
                    }
                    decideWhatToInsertForKit(kit, outboundKit, false);
                }
                kit.setPackageId(null);
                Kit inboundKit = getDDBKitBasedOnKitLabel(SQL_SELECT_KIT_BY_KIT_LABEL + SQL_SELECT_INBOUND, kit.getKitLabel(), ddpInstance.getDdpInstanceId());
                if (inboundKit == null) {
                    logger.error("Inbound Kit found for " + kit.getKitLabel() + " is null!");
                }
                else {
                    if (kit.getGuid() != inboundKit.getGuid() || kit.getShortId() != inboundKit.getShortId()) {
                        throw new RuntimeException("2 kits don't match for inbound for kitLabel " + kit.getKitLabel() + " file kit is for participant (" + kit.getShortId() + "," + kit.getGuid() + ") and DB kit is for participant (" + inboundKit.getShortId() + "," + inboundKit.getGuid() + ")");
                    }
                    decideWhatToInsertForKit(kit, inboundKit, true);
                }
            }
        }
    }

    public static void decideWhatToInsertForKit(Kit fileKit, Kit dbKit, boolean isRetrun) {
        fileKit.setDsmKitRequestId(dbKit.getDsmKitRequestId());
        fileKit.setTrackingToId(dbKit.getTrackingToId());
        fileKit.setTrackingReturnId(dbKit.getTrackingReturnId());
        fileKit.setShipmentId(dbKit.getShipmentId());
        fileKit.setPackageId(dbKit.getPackageId());
        if (StringUtils.isBlank(dbKit.getLastActivityDesc())) {
            logger.info("Inserting Shipment, Package and all activities for kit " + fileKit.getKitLabel());
            insertKitInDB(fileKit, true);
        }
        else {
            if (StringUtils.isBlank(fileKit.getPackageId()) || StringUtils.isBlank(fileKit.getShipmentId())) {
                throw new RuntimeException("No package Id or Shipment Id was found for a kit with activity in DB! Kit label is " + fileKit.getKitLabel());
            }
            if (StringUtils.isNotBlank(dbKit.getLastActivityDateTime())) {
                String firstActivityTime = isRetrun ? fileKit.getPickedUpAt() : fileKit.getShippedAt();
                if (StringUtils.isNotBlank(firstActivityTime)) {
                    if (shouldInsertBasedOnTimeForKit(dbKit, firstActivityTime)) {//last activity in DB is before the shipped at
                        insertActivityForPackage(fileKit.getPackageId(), "Picked up", firstActivityTime);
                    }
                    else {
                        String lastActivityTime = isRetrun ? fileKit.getDeliveredAt() : fileKit.getReceivedAt();
                        if (StringUtils.isNotBlank(lastActivityTime)) {
                            if (shouldInsertBasedOnTimeForKit(dbKit, lastActivityTime)) {//last activity in DB is before the shipped at
                                insertActivityForPackage(fileKit.getPackageId(), "Delivered", lastActivityTime);
                            }
                        }
                    }
                }
            }
            else {
                throw new RuntimeException("No package Id or Shipment Id was set for a kit with activity in DB! Kit label is " + fileKit.getKitLabel());
            }
        }

    }

    private static boolean shouldInsertBasedOnTimeForKit(@NonNull Kit dbKit, String utcDateTimeString) {
        Instant activityInDBInstant = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.of("UTC")).parse(dbKit.getLastActivityDateTime(), Instant::from);
        String fileDateTime = utcDateTimeString.replace("T", " ").replace("Z", "");
        Instant fileInstant = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.of("UTC")).parse(fileDateTime, Instant::from);
        boolean isBefore = activityInDBInstant.isBefore(fileInstant);
        return isBefore;
    }

    public static void insertKitInDB(Kit kit, boolean isReturn) {
        logger.info("Inserting new " + (isReturn ? "inbound" : "outbound") + " kit information for kit " + kit.getDsmKitRequestId());
        if (!isReturn && StringUtils.isBlank(kit.getShipmentId())) {
            kit.setShipmentId(insertShipmentForKit(kit.getDsmKitRequestId()));
        }
        if (StringUtils.isBlank(kit.getPackageId())) {
            String trackingNumber = isReturn ? kit.getTrackingReturnId() : kit.getTrackingToId();
            kit.setPackageId(insertPackageForKit(kit, trackingNumber));
        }
        if (!isReturn) {
            if (StringUtils.isNotBlank(kit.getShippedAt())) {
                insertActivityForPackage(kit.getPackageId(), "Picked up", kit.getShippedAt());
            }
            if (StringUtils.isNotBlank(kit.getDeliveredAt())) {
                insertActivityForPackage(kit.getPackageId(), "Delivered", kit.getDeliveredAt());
            }
        }
        else {
            if (StringUtils.isNotBlank(kit.getPickedUpAt())) {
                insertActivityForPackage(kit.getPackageId(), "Picked up", kit.getPickedUpAt());
            }
            if (StringUtils.isNotBlank(kit.getReceivedAt())) {
                insertActivityForPackage(kit.getPackageId(), "Delivered", kit.getReceivedAt());
            }
        }

    }

    public static void insertActivityForPackage(String packageId, String status, String timeStamp) {
        String sqlDateTime = getSQLDateTimeString(timeStamp);// we need EST time in DB
        SimpleResult results = inTransaction((conn) -> {
            SimpleResult dbVals = new SimpleResult();
            try (PreparedStatement insertStmt = conn.prepareStatement(SQL_INSERT_ACTIVITY)) {
                insertStmt.setString(1, packageId);
                insertStmt.setString(2, status);
                insertStmt.setString(3, sqlDateTime);
                insertStmt.executeUpdate();
            }
            catch (SQLException ex) {
                logger.error("Error inserting new activity for package " + packageId);
                dbVals.resultException = ex;
            }
            return dbVals;
        });

        if (results.resultException != null) {
            throw new RuntimeException("Error getting list of realms ", results.resultException);
        }
    }

    public static String insertShipmentForKit(String dsmKitRequestId) {
        SimpleResult results = inTransaction((conn) -> {
            SimpleResult dbVals = new SimpleResult();
            try (PreparedStatement insertStmt = conn.prepareStatement(SQL_INSERT_SHIPMENT, Statement.RETURN_GENERATED_KEYS)) {
                insertStmt.setString(1, dsmKitRequestId);
                insertStmt.executeUpdate();
                try (ResultSet rs = insertStmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        dbVals.resultValue = rs.getString(1);
                    }
                }
                catch (Exception e) {
                    throw new RuntimeException("Error getting the id of inserted shipment for dsm_kit_request_id " + dsmKitRequestId, e);
                }
            }
            catch (SQLException ex) {
                logger.error("Error inserting shipment for dsm_kit_request_id " + dsmKitRequestId);
                dbVals.resultException = ex;
            }
            return dbVals;
        });

        if (results.resultException != null) {
            throw new RuntimeException("Error getting list of realms ", results.resultException);
        }

        return (String) results.resultValue;
    }

    public static String insertPackageForKit(Kit kit, String trackingNumber) {
        logger.info("Inserting new package information for kit " + kit.getDsmKitRequestId());
        SimpleResult results = inTransaction((conn) -> {
            SimpleResult dbVals = new SimpleResult();
            try (PreparedStatement insertStmt = conn.prepareStatement(SQL_INSERT_PACKAGE, Statement.RETURN_GENERATED_KEYS)) {
                insertStmt.setString(1, kit.getShipmentId());
                insertStmt.setString(2, trackingNumber);
                insertStmt.executeUpdate();
                try (ResultSet rs = insertStmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        dbVals.resultValue = rs.getString(1);
                    }
                }
                catch (Exception e) {
                    throw new RuntimeException("Error getting the id of inserted package for shipment " + kit.getShipmentId(), e);
                }
            }
            catch (SQLException ex) {
                logger.error("Error inserting package for shipment " + kit.getShipmentId());
                dbVals.resultException = ex;
            }
            return dbVals;
        });

        if (results.resultException != null) {
            throw new RuntimeException("Error getting list of realms ", results.resultException);
        }

        return (String) results.resultValue;
    }

    public static Kit getDDBKitBasedOnKitLabel(String query, String kitLabel, String ddpInstanceId) {
        SimpleResult results = inTransaction((conn) -> {
            SimpleResult dbVals = new SimpleResult();
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, ddpInstanceId);
                stmt.setString(2, kitLabel);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        Kit kit = new Kit(
                                rs.getString("kit." + DBConstants.KIT_LABEL),
                                rs.getString(DBConstants.UPLOAD_REASON),
                                rs.getString(DBConstants.KIT_TEST_RESULT),
                                rs.getString(DBConstants.CREATED_DATE),
                                "",
                                "",
                                "",
                                "",
                                "",
                                rs.getString(DBConstants.DSM_KIT_REQUEST_ID),
                                rs.getString(DBConstants.DSM_TRACKING_TO),
                                rs.getString(DBConstants.TRACKING_RETURN_ID),
                                rs.getString(DBConstants.UPS_ACTIVITY_DATE_TIME),
                                rs.getString(DBConstants.UPS_STATUS_DESCRIPTION),
                                rs.getString(DBConstants.UPS_SHIPMENT_ID),
                                rs.getString(DBConstants.UPS_PACKAGE_ID),
                                rs.getString(DBConstants.COLLABORATOR_PARTICIPANT_ID),
                                rs.getString(DBConstants.DDP_PARTICIPANT_ID)
                        );
                        dbVals.resultValue = kit;
                    }
                }
                catch (Exception ex) {
                    dbVals.resultException = ex;
                }
            }
            catch (Exception ex) {
                dbVals.resultException = ex;
            }
            return dbVals;
        });

        if (results.resultException != null) {
            throw new RuntimeException("Error getting Kit with kitLabel " + kitLabel, results.resultException);

        }
        Kit kit = (Kit) results.resultValue;
        if (kit == null) {
            logger.error("Kit found for " + kitLabel + " is null!");
        }
        logger.info("Found Kit with KitLabel " + kitLabel + " with dsm_kit_request_id " + kit.getDsmKitRequestId());
        return kit;
    }

    public static String getSQLDateTimeString(String dateTime) {
        dateTime = dateTime.replace("T", " ").replace("Z", "");
        Instant activityInstant = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.of("UTC")).parse(dateTime, Instant::from);
        DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.of("America/New_York"));
        String activityDateTime = DATE_TIME_FORMATTER.format(activityInstant);
        return activityDateTime;
    }

    @Test
    public static void InsertFile() {
        Map<String, ArrayList<Kit>> participants = readFile("");
        insertFileInfo(participants);
    }

}
