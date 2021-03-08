package org.broadinstitute.dsm.db;

import com.google.gson.Gson;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.dsm.model.ups.UPSActivity;
import org.broadinstitute.dsm.model.ups.UPSStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@Data
public class DdpKit {
    String DsmKitRequestId;
    String kitLabel;
    String trackingToId;
    String trackingReturnId;
    String error;
    String message;
    String receiveDate;
    String upsTrackingStatus;
    String upsTrackingDate;
    String upsReturnStatus;
    String upsReturnDate;
    String HRUID;
    String externalOrderNumber;
    String kitShippingHistory;
    String kitReturnHistory;
    boolean CEOrdered;
    String ddpInstanceId;

    private static final Logger logger = LoggerFactory.getLogger(DdpKit.class);

    public DdpKit(String DsmKitRequestId, String kitLabel, String trackingToId, String trackingReturnId, String error,
                  String message, String receiveDate, String bspCollaboratodId, String externalOrderNumber,
                  boolean CEOrdered, String kitShippingHistory, String kitReturnHistory, String ddpInstanceId) {
        this.DsmKitRequestId = DsmKitRequestId;
        this.kitLabel = kitLabel;
        this.trackingToId = trackingToId;
        this.trackingReturnId = trackingReturnId;
        this.error = error;
        this.message = message;
        this.receiveDate = receiveDate;
        this.HRUID = bspCollaboratodId;
        this.externalOrderNumber = externalOrderNumber;
        this.CEOrdered = CEOrdered;
        this.kitShippingHistory = kitShippingHistory;
        this.kitReturnHistory = kitReturnHistory;
        this.ddpInstanceId = ddpInstanceId;
    }

    public void changeCEOrdered(Connection conn, boolean orderStatus) {
        String query = "UPDATE ddp_kit SET CE_order = ? where dsm_kit_request_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setBoolean(1, orderStatus);
            stmt.setString(2, this.getDsmKitRequestId());
            int r = stmt.executeUpdate();
            if (r != 1) {//number of subkits
                throw new RuntimeException("Update query for CE order flag updated " + r + " rows! with dsm kit request id: " + this.getDsmKitRequestId());
            }
            logger.info("Updated CE_Order value for kit with dsm kit request id " + this.getDsmKitRequestId()
                    + " to " + orderStatus);
        }
        catch (Exception e) {
            throw new RuntimeException("Could not update ce_ordered status for " + this.getDsmKitRequestId(), e);
        }
    }

    public static boolean hasKitBeenOrderedInCE(Connection conn, String kitLabel) {
        String query = "select k.ce_order from ddp_kit k where k.kit_label = ?";
        boolean hasBeenOrdered = false;
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, kitLabel);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                hasBeenOrdered = rs.getBoolean(1);
                if (rs.next()) {
                    throw new RuntimeException("Too many rows found for kit " + kitLabel);
                }
            }
            else {
                throw new RuntimeException("Could not find kit " + kitLabel);
            }

        }
        catch (Exception e) {
            throw new RuntimeException("Could not determine ce_ordered status for " + kitLabel, e);
        }
        return hasBeenOrdered;
    }

    public static void updateCEOrdered(Connection conn, boolean ordered, String kitLabel) {
        String query = "UPDATE ddp_kit SET CE_order = ? where kit_label = ?";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setBoolean(1, ordered);
            stmt.setString(2, kitLabel);
            int r = stmt.executeUpdate();
            if (r != 1) {//number of subkits
                throw new RuntimeException("Update query for CE order flag updated " + r + " rows! with dsm kit " + kitLabel);
            }
        }
        catch (Exception e) {
            throw new RuntimeException("Could not update ce_ordered status for " + kitLabel, e);
        }
    }

    public boolean isDelivered() {
        if (StringUtils.isNotBlank(trackingToId)) {
            return isTypeDelivered(upsTrackingStatus);
        }
        return false;
    }

    public boolean isReturned() {
        if (StringUtils.isNotBlank(trackingReturnId)) {
            return isTypeDelivered(upsReturnStatus);
        }
        return false;
    }

    private boolean isTypeDelivered(String upsHistory) {
        if (StringUtils.isNotBlank(upsHistory)) {
            UPSActivity[] activities = new Gson().fromJson(upsHistory, UPSActivity[].class);
            if (activities != null) {
                UPSActivity lastActivity = activities[0];
                if (lastActivity != null) {
                    String type = lastActivity.getStatus().getType();
                    return UPSStatus.DELIVERED_TYPE.equals(type);
                }
            }
        }
        return false;
    }

    public String getMainKitLabel() {
        if (StringUtils.isNotBlank(kitLabel) && kitLabel.contains("_1") && kitLabel.indexOf("_1") == kitLabel.length() - 2) {
            return kitLabel.substring(0, kitLabel.length() - 2);
        }
        return kitLabel;
    }

}
