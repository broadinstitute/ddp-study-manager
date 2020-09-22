package org.broadinstitute.dsm.db;

import lombok.Data;
import org.broadinstitute.ddp.db.SimpleResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.PreparedStatement;

import static org.broadinstitute.ddp.db.TransactionWrapper.inTransaction;

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

    public void changeCEOrdered(boolean orderStatus) {
        String query = "UPDATE ddp_kit SET CE_order = ? where dsm_kit_request_id = ?";
        SimpleResult result = inTransaction((conn) -> {
            SimpleResult dbVals = new SimpleResult();
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setBoolean(1, orderStatus);
                stmt.setString(2, this.getDsmKitRequestId());
                int r = stmt.executeUpdate();
                if (r != 1) {//number of subkits
                    throw new RuntimeException("Update query for CE order flag updated " + r + " rows! with dsm kit request id: " + this.getDsmKitRequestId());
                }
            }
            catch (Exception e) {
                dbVals.resultException = e;
            }
            return dbVals;
        });
        if (result.resultException != null) {
            throw new RuntimeException(result.resultException);
        }
        else {
            logger.info("Updated CE_Order value for kit with dsm kit request id " + this.getDsmKitRequestId()
                    + " to " + orderStatus);
        }
    }

}
