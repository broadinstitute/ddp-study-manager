package org.broadinstitute.dsm.careevolve;

import com.google.gson.annotations.SerializedName;

public class Provider {

    @SerializedName("FirstName")
    private String firstName;

    @SerializedName("LastName")
    private String lastName;
    private final String npi;

    @SerializedName("NPI")
    private String NPI;

    public Provider(String firstName,
                    String lastName,
                    String npi) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.npi = npi;
    }
}
