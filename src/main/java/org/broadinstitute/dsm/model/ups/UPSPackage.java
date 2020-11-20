package org.broadinstitute.dsm.model.ups;

import lombok.Data;

@Data
public class UPSPackage {
    String trackingNumber;
    UPSActivity[] activity;

    public UPSActivity getEarliestIndicationOfInTransit() {
        UPSActivity earliestReturnEvent = null;
        if (activity != null) {
            for (UPSActivity event : activity) {
                if (earliestReturnEvent == null) {
                    earliestReturnEvent = event;
                } else {
                    if (event.isInTransit() || event.isDelivered()) {
                        if (event.getInstant().isBefore(earliestReturnEvent.getInstant())) {
                            earliestReturnEvent = event;
                        }
                    }
                }
            }
        }
        return earliestReturnEvent;
    }

}
