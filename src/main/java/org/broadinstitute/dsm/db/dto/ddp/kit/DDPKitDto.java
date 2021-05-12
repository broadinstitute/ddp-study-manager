package org.broadinstitute.dsm.db.dto.ddp.kit;

import lombok.Data;

@Data
public class DDPKitDto {

    private int dsmKitId;
    private int dsmKitRequestId;
    private String trackingToId;
    private String trackingReturnId;
    private String upsTrackingDate;
    private String upsReturnDate;
    private String upsTrackingStatus;
    private String upsReturnStatus;
    private String kitShippingHistory;

    public DDPKitDto(int dsmKitId, int dsmKitRequestId, String trackingToId, String trackingReturnId, String upsTrackingDate,
                     String upsReturnDate, String upsTrackingStatus, String upsReturnStatus, String kitShippingHistory) {
        this.dsmKitId = dsmKitId;
        this.dsmKitRequestId = dsmKitRequestId;
        this.trackingToId = trackingToId;
        this.trackingReturnId = trackingReturnId;
        this.upsTrackingDate = upsTrackingDate;
        this.upsReturnDate = upsReturnDate;
        this.upsTrackingStatus = upsTrackingStatus;
        this.upsReturnStatus = upsReturnStatus;
        this.kitShippingHistory = kitShippingHistory;
    }
}
