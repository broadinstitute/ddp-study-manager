package org.broadinstitute.dsm.util.tools;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.ddp.db.SimpleResult;
import org.broadinstitute.dsm.db.DDPInstance;
import org.broadinstitute.dsm.statics.DBConstants;
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
    private static final Logger logger = LoggerFactory.getLogger(TbosUPSKitTool.class);
    private static final String SQL_SELECT_KIT_BY_KIT_LABEL = "Select kit.dsm_kit_request_id, req.*, activity.*,  pack.tracking_number " +
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
    private static final String SQL_SELECT_OUTBOUND = "and pack.tracking_number = kit.tracking_to_id ";
    private static final String SQL_SELECT_INBOUND = "and pack.tracking_number = kit.tracking_return_id ";
    private static final String SQL_INSERT_SHIPMENT = "INSERT INTO ups_shipment ( dsm_kit_request_id ) VALUES (?) ";
    private static final String SQL_INSERT_PACKAGE = "INSERT INTO ups_package ( ups_shipment_id , tracking_number ) VALUES (?, ?) ";
    private static final String SQL_INSERT_ACTIVITY = "INSERT INTO ups_activity ( ups_package_id, ups_status_description, ups_activity_date_time) VALUES (?, ?, ?)";

    private static Map readFile(String fileName) {
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
                if (kit != null && StringUtils.isNotBlank(shortId)) {// last kit of the participant in the last line
                    if (!participants.containsKey(shortId)) {
                        participants.put(shortId, new ArrayList<Kit>());
                    }
                    ArrayList<Kit> list = participants.get(shortId);
                    list.add(kit);
                    participants.put(shortId, list);
                    kit = null;
                }
                String participantLine = scanner.nextLine();
                String[] participantInfo = participantLine.split(",");
                String guid = participantInfo[0]; //read first cell
                shortId = participantInfo[1]; //read second cell
                int i = 2;
                for (String header : headers) {
                    String value = participantInfo[i];
                    if (StringUtils.isBlank(value) || "-".equals(value)) {
                        i++;
                        continue;
                    }
                    if ("kit label".equals(header)) {
                        if (kit != null) {
                            if (!participants.containsKey(shortId)) {
                                participants.put(shortId, new ArrayList<Kit>());
                            }
                            ArrayList<Kit> list = participants.get(shortId);
                            list.add(kit);
                            participants.put(shortId, list);
                            kit = null;
                        }
                    }
                    if (kit == null) {
                        kit = new Kit();
                        kit.shortId = shortId;
                        kit.guid = guid;
                    }
                    kit.setValue(header, value);
                    i++;
                }
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
                Kit outboundKit = getDDBKitBasedOnKitLabel(SQL_SELECT_KIT_BY_KIT_LABEL + SQL_SELECT_OUTBOUND, kit.kitLabel, ddpInstance.getDdpInstanceId());
                //todo check if matches the kit in details
                if (kit.guid != outboundKit.guid || kit.shortId != outboundKit.shortId) {
                    throw new RuntimeException("2 kits don't match for outbound for kitLabel " + kit.kitLabel + " file kit is for participant (" + kit.shortId + "," + kit.guid + ") and DB kit is for participant (" + outboundKit.shortId + "," + outboundKit.guid + ")");
                }
                kit.dsmKitRequestId = outboundKit.dsmKitRequestId;
                kit.trackingToId = outboundKit.trackingToId;
                kit.shipmentId = outboundKit.shipmentId;
                kit.packageId = outboundKit.packageId;
                if (StringUtils.isBlank(outboundKit.lastActivityDesc)) {
                    insertValuesForKit(kit, false);
                }
                kit.packageId = null;
                Kit inboundKit = getDDBKitBasedOnKitLabel(SQL_SELECT_KIT_BY_KIT_LABEL + SQL_SELECT_INBOUND, kit.kitLabel, ddpInstance.getDdpInstanceId());
                //todo check if matches the kit in details
                if (kit.guid != inboundKit.guid || kit.shortId != inboundKit.shortId) {
                    throw new RuntimeException("2 kits don't match for inbound for kitLabel " + kit.kitLabel + " file kit is for participant (" + kit.shortId + "," + kit.guid + ") and DB kit is for participant (" + inboundKit.shortId + "," + inboundKit.guid + ")");
                }
                kit.dsmKitRequestId = inboundKit.dsmKitRequestId;
                kit.trackingReturnId = inboundKit.trackingReturnId;
                kit.shipmentId = inboundKit.shipmentId;
                kit.packageId = inboundKit.packageId;
                if (StringUtils.isBlank(inboundKit.lastActivityDesc)) {
                    insertValuesForKit(kit, true);
                }
            }
        }
    }

    private static void insertValuesForKit(Kit kit, boolean isReturn) {
        logger.info("Inserting new kit information for kit " + kit.dsmKitRequestId);
        if (!isReturn && StringUtils.isBlank(kit.shipmentId)) {
            kit.shipmentId = insertShipmentForKit(kit.dsmKitRequestId) + "";
        }
        if (StringUtils.isBlank(kit.packageId)) {
            String trackingNumber = isReturn ? kit.trackingReturnId : kit.trackingToId;
            kit.packageId = insertPackageForKit(kit, trackingNumber) + "";
        }
        if (!isReturn) {
            insertActivityForPackage(kit.packageId, "shipped", kit.shippedAt);
            insertActivityForPackage(kit.packageId, "Delivered AT", kit.deliveredAt);
        }
        else {
            insertActivityForPackage(kit.packageId, "Picked up", kit.pickedUpAt);
            insertActivityForPackage(kit.packageId, "Delivered AT", kit.receivedAt);
        }

    }

    private static void insertActivityForPackage(String packageId, String status, String timeStamp) {
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

    private static int insertShipmentForKit(String dsmKitRequestId) {
        SimpleResult results = inTransaction((conn) -> {
            SimpleResult dbVals = new SimpleResult();
            try (PreparedStatement insertStmt = conn.prepareStatement(SQL_INSERT_SHIPMENT, Statement.RETURN_GENERATED_KEYS)) {
                insertStmt.setString(1, dsmKitRequestId);
                insertStmt.executeUpdate();
                try (ResultSet rs = insertStmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        dbVals.resultValue = rs.getInt(1);
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

        return (int) results.resultValue;
    }

    private static int insertPackageForKit(Kit kit, String trackingNumber) {
        logger.info("Inserting new package information for kit " + kit.dsmKitRequestId);
        SimpleResult results = inTransaction((conn) -> {
            SimpleResult dbVals = new SimpleResult();
            try (PreparedStatement insertStmt = conn.prepareStatement(SQL_INSERT_PACKAGE, Statement.RETURN_GENERATED_KEYS)) {
                insertStmt.setString(1, kit.shipmentId);
                insertStmt.setString(2, trackingNumber);
                insertStmt.executeUpdate();
                try (ResultSet rs = insertStmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        dbVals.resultValue = rs.getInt(1);
                    }
                }
                catch (Exception e) {
                    throw new RuntimeException("Error getting the id of inserted package for shipment " + kit.shipmentId, e);
                }
            }
            catch (SQLException ex) {
                logger.error("Error inserting package for shipment " + kit.shipmentId);
                dbVals.resultException = ex;
            }
            return dbVals;
        });

        if (results.resultException != null) {
            throw new RuntimeException("Error getting list of realms ", results.resultException);
        }

        return (int) results.resultValue;
    }

    public static void main(String[] args) {
        Map<String, ArrayList<Kit>> participants = readFile("");

    }

    private static Kit getDDBKitBasedOnKitLabel(String query, String kitLabel, String ddpInstanceId) {

        SimpleResult results = inTransaction((conn) -> {
            SimpleResult dbVals = new SimpleResult();
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, ddpInstanceId);
                stmt.setString(2, kitLabel);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        Kit kit = new Kit(
                                DBConstants.KIT_LABEL,
                                DBConstants.UPLOAD_REASON,
                                DBConstants.KIT_TEST_RESULT,
                                DBConstants.CREATED_DATE,
                                "",
                                "",
                                "",
                                "",
                                "",
                                DBConstants.DSM_KIT_REQUEST_ID,
                                DBConstants.DSM_TRACKING_TO,
                                DBConstants.TRACKING_RETURN_ID,
                                DBConstants.UPS_ACTIVITY_DATE_TIME,
                                DBConstants.UPS_STATUS_DESCRIPTION,
                                DBConstants.UPS_SHIPMENT_ID,
                                DBConstants.UPS_PACKAGE_ID,
                                DBConstants.COLLABORATOR_PARTICIPANT_ID,
                                DBConstants.PARTICIPANT_ID
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
        logger.info("Found Kit with KitLabel " + kitLabel + " with dsm_kit_request_id " + kit.dsmKitRequestId);
        return kit;

    }

    public static String getSQLDateTimeString(String dateTime) {
        Instant activityInstant = DateTimeFormatter.ofPattern("yyyy-MM-ddTHH:mm:ss.fffV").withZone(ZoneId.of("UTC")).parse(dateTime, Instant::from);
        DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.of("America/New_York"));
        String activityDateTime = DATE_TIME_FORMATTER.format(activityInstant);
        return activityDateTime;
    }
}

class Kit {
    private String kitLabelHeader = "kit label";
    private String reasonHeader = "";
    private String resultHeader = "";
    private String requestedAtHeader = "";
    private String shippedAtHeader = "";
    private String deliveredAtHeader = "";
    private String pickedUpAtHeader = "";
    private String receivedAtHeader = "";
    private String resultedAtHeader = "";

    String kitLabel;
    String reason;
    String result;
    String requestedAt;
    String shippedAt;
    String deliveredAt;
    String pickedUpAt;
    String receivedAt;
    String resultedAt;
    String dsmKitRequestId;
    String trackingToId;
    String trackingReturnId;
    String lastActivityDateTime;
    String lastActivityDesc;
    String shipmentId;
    String packageId;
    String shortId;
    String guid;

    public Kit() {
    }

    public Kit(String kitLabel,
               String reason,
               String result,
               String requestedAt,
               String shippedAt,
               String deliveredAt,
               String pickedUpAt,
               String receivedAt,
               String resultedAt,
               String dsmKitRequestId,
               String trackingToId,
               String trackingReturnId,
               String lastActivityDateTime,
               String lastActivityDesc,
               String shipmentId,
               String packageId,
               String shortId,
               String guid) {
        this.kitLabel = kitLabel;
        this.reason = reason;
        this.result = result;
        this.requestedAt = requestedAt;
        this.shippedAt = shippedAt;
        this.deliveredAt = deliveredAt;
        this.pickedUpAt = pickedUpAt;
        this.receivedAt = receivedAt;
        this.resultedAt = resultedAt;
        this.dsmKitRequestId = dsmKitRequestId;
        this.trackingToId = trackingToId;
        this.trackingReturnId = trackingReturnId;
        this.lastActivityDateTime = lastActivityDateTime;
        this.lastActivityDesc = lastActivityDesc;
        this.shipmentId = shipmentId;
        this.packageId = packageId;
        this.shortId = shortId;
        this.guid = guid;
    }

    public void setValue(String header, String value) {
        switch (header) {
            case "kit label":
                this.kitLabel = value;
                break;
            case "reason":
                this.reason = value;
                break;
            case "result":
                this.result = value;
                break;
            case "requested at":
                this.requestedAt = value;
                break;
            case "shipped at":
                this.shippedAt = value;
                break;
            case "delivered at":
                this.deliveredAt = value;
                break;
            case "picked up at":
                this.pickedUpAt = value;
                break;
            case "received at":
                this.receivedAt = value;
                break;
            case "resulted at":
                this.resultedAt = value;
                break;
            default:
                throw new IllegalStateException("Unexpected header, value: " + header + "," + value);
        }
    }
}
