package org.broadinstitute.dsm.model.ups;

import lombok.Data;

@Data
public class UPSStatus {
    String type;
    String description;
    String code;

    public static final String IN_TRANSIT_CODE = "I";

    public static final String DELIVERED_CODE = "D";

    public UPSStatus() {}

    public UPSStatus(String type, String description, String code) {
        this.type = type;
        this.description = description;
        this.code = code;
    }

    public boolean isInTransit() {
        return IN_TRANSIT_CODE.equals(code);
    }

    public boolean isDelivered() {
        return DELIVERED_CODE.equals(code);
    }
}
