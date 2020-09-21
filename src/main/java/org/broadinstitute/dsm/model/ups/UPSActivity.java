package org.broadinstitute.dsm.model.ups;

import lombok.Data;

@Data
public class UPSActivity {
    UPSLocation location;
    UPSStatus status;
    String date;
    String time;
}
