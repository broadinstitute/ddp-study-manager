package org.broadinstitute.dsm.util.model;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;

@Data
public class Kit {

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

    public void setValueByHeader(String header, String value) {
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

    public boolean isEmpty() {
        if (StringUtils.isBlank(kitLabel) &&
                StringUtils.isBlank(reason) &&
                StringUtils.isBlank(result) &&
                StringUtils.isBlank(requestedAt) &&
                StringUtils.isBlank(shippedAt) &&
                StringUtils.isBlank(deliveredAt) &&
                StringUtils.isBlank(pickedUpAt) &&
                StringUtils.isBlank(receivedAt) &&
                StringUtils.isBlank(resultedAt)){
            return true;
        }
        return false;
    }
}
