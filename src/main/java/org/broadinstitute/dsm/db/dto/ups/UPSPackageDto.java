package org.broadinstitute.dsm.db.dto.ups;

import lombok.Data;

@Data
public class UPSPackageDto {

    private int upsPackageId;
    private int upsShipmentId;
    private String trackingNumber;
    private String deliveryDate;

    public UPSPackageDto(int upsPackageId, int upsShipmentId, String trackingNumber, String deliveryDate) {
        this.upsPackageId = upsPackageId;
        this.upsShipmentId = upsShipmentId;
        this.trackingNumber = trackingNumber;
        this.deliveryDate = deliveryDate;
    }

    public UPSPackageDto(int upsPackageId, int upsShipmentId, String trackingNumber) {
        this.upsPackageId = upsPackageId;
        this.upsShipmentId = upsShipmentId;
        this.trackingNumber = trackingNumber;
    }
}
