package org.broadinstitute.dsm.careevolve;

import com.google.gson.annotations.SerializedName;

public class Address {

    @SerializedName("Line1")
    private String line1;

    @SerializedName("Line2")
    private String line2;

    @SerializedName("City")
    private String city;

    @SerializedName("State")
    private String state;

    @SerializedName("ZipCode")
    private String zipCode;

    public Address(String line1, String line2, String city, String state, String zipCode) {
        this.line1 = line1;
        this.line2 = line2;
        this.city = city;
        this.state = state;
        this.zipCode = zipCode;
    }
}
