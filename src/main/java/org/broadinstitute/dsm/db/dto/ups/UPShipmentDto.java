package org.broadinstitute.dsm.db.dto.ups;

import lombok.Data;

@Data
public class UPShipmentDto {

    private int upsShipmentId;
    private int dsmKitRequestId;

    public UPShipmentDto(int upsShipmentId, int dsmKitRequestId) {
        this.upsShipmentId = upsShipmentId;
        this.dsmKitRequestId = dsmKitRequestId;
    }

    public UPShipmentDto() {

    }
}
