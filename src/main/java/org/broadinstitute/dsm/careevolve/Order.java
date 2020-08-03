package org.broadinstitute.dsm.careevolve;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.annotations.SerializedName;

public class Order {

    @SerializedName("AOEs")
    private List<AOE> aoes = new ArrayList<>();

    @SerializedName("collection")
    private String collectionTime;

    @SerializedName("patient")
    private Patient patient;

    @SerializedName("provider")
    private Provider provider;

    /**
     * This is the label on the tube that will
     * be scanned into Mercury during accessioning
     */
    @SerializedName("OrderId")
    private String kitLabel;

    @SerializedName("CareEvolveAccount")
    private String account;

    public Order(String account, Patient patient, String kitLabel,Provider provider, List<AOE> aoes) {
        this.account = account;
        this.provider = provider;
        this.aoes = aoes;
        this.patient = patient;
        this.kitLabel = kitLabel;
    }

    public String getOrderId() {
        return kitLabel;
    }
}
