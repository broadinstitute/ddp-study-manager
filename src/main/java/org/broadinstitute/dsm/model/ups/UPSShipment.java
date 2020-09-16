package org.broadinstitute.dsm.model.ups;

import com.google.gson.annotations.SerializedName;

public class UPSShipment {
    @SerializedName("package")
    UPSPackage[] upsPackageArray;

}
class UPSPackage{
    String trackingNumber;
    UPSActivity[] activity;
}
class UPSActivity{
    UPSLocation location;
    UPSStatus status;
    String date;
    String time;
}
class UPSLocation{
//address is here but I don't think we care about that
}
class UPSStatus{
    String type;
    String description;
    String code;
}

