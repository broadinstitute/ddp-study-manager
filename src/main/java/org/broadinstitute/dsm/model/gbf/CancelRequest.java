package org.broadinstitute.dsm.model.gbf;

import com.google.gson.annotations.SerializedName;

public class CancelRequest {

    @SerializedName("orderNumber")
    private String orderNumber;

    public CancelRequest(String orderNumber) {
        this.orderNumber = orderNumber;
    }
}
