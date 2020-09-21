package org.broadinstitute.dsm.db;

import lombok.Data;

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

    public DdpKit(String DsmKitRequestId, String kitLabel, String trackingToId, String trackingReturnId, String error,
                  String message, String receiveDate, String upsTrackingStatus, String upsTrackingDate,
                  String upsReturnStatus, String upsReturnDate, String bspCollaboratodId, String externalOrderNumber) {
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
        this.HRUID = bspCollaboratodId.substring(bspCollaboratodId.indexOf('_') + 1, bspCollaboratodId.lastIndexOf('_'));
        this.externalOrderNumber = externalOrderNumber;

    }

}
