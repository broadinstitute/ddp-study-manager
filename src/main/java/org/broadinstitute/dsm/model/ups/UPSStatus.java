package org.broadinstitute.dsm.model.ups;

import lombok.Data;

@Data
public class UPSStatus {
    String type;
    String description;
    String code;
}
