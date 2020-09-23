package org.broadinstitute.dsm.model.ups;

import lombok.Data;

@Data
public class UPSPackage{
    String trackingNumber;
    UPSActivity[] activity;
}
