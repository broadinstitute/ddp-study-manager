package org.broadinstitute.dsm.model.gbf;

import com.google.gson.annotations.SerializedName;

public class CancelResponse {

    @SerializedName("orderNumber")
    private String orderNumber;

    @SerializedName("orderStatus")
    private String orderStatus;

    @SerializedName("success")
    private Boolean success;

    @SerializedName("errorMessage")
    private String errorMessage;

    public String getOrderNumber() {
        return orderNumber;
    }

    public String getOrderStatus() {
        return orderStatus;
    }

    public boolean wasSuccessful() {
        return Boolean.TRUE.equals(success);
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
