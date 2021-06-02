package org.broadinstitute.dsm.model.ups;

import com.google.gson.Gson;
import lombok.Data;

@Data
public class UPSLocation {
    UPSAddress address;

    public UPSLocation() {}

    public UPSLocation(UPSAddress address) {
        this.address = address;
    }

    public String getString() {
        return new Gson().toJson(this);
    }
}
