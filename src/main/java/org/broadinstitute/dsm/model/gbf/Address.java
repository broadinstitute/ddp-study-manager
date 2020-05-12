package org.broadinstitute.dsm.model.gbf;

import javax.xml.bind.annotation.XmlElement;

public class Address {

    @XmlElement(name="Company")
    private String company;

    @XmlElement(name="AddressLine1")
    private String addressLine1;

    @XmlElement(name="AddressLine2")
    private String addressLine2;

    @XmlElement(name="City")
    private String city;

    @XmlElement(name="State")
    private String state;

    @XmlElement(name="ZipCode")
    private String zipCode;

    @XmlElement(name="Country")
    private String country;

    @XmlElement(name="PhoneNumber")
    private String phoneNumber;

    public Address() {
    }

    public Address(String company, String addressLine1, String addressLine2, String city, String state, String zipCode,
                   String country, String phoneNumber) {
        this.company = company;
        this.addressLine1 = addressLine1;
        this.addressLine2 = addressLine2;
        this.city = city;
        this.state = state;
        this.zipCode = zipCode;
        this.country = country;
        this.phoneNumber = phoneNumber;
    }
}
