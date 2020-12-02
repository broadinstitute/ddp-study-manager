package org.broadinstitute.dsm.db;

import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

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
    boolean CEOrdered;
    private static final Logger logger = LoggerFactory.getLogger(DdpKit.class);

    public DdpKit(String DsmKitRequestId, String kitLabel, String trackingToId, String trackingReturnId, String error,
                  String message, String receiveDate, String upsTrackingStatus, String upsTrackingDate,
                  String upsReturnStatus, String upsReturnDate, String bspCollaboratodId, String externalOrderNumber,
                  boolean CEOrdered) {
        this.DsmKitRequestId = DsmKitRequestId;
        this.kitLabel = kitLabel;
        this.trackingToId = trackingToId;
        this.trackingReturnId = trackingReturnId;
        this.error = error;
        this.message = message;
        this.receiveDate = receiveDate;
        this.upsTrackingStatus = upsTrackingStatus;
        this.upsTrackingDate = upsTrackingDate;
        this.upsReturnStatus = upsReturnStatus;
        this.upsReturnDate = upsReturnDate;
        this.HRUID = bspCollaboratodId;
        this.externalOrderNumber = externalOrderNumber;
        this.CEOrdered = CEOrdered;
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
        }
        catch (SQLException e) {
            throw new RuntimeException("Could not update CE order status to " + orderStatus + " for kit " + kitLabel);
        }
        logger.info("Updated CE_Order value for kit with dsm kit request id " + this.getDsmKitRequestId() + " to " + orderStatus);
    }

}
