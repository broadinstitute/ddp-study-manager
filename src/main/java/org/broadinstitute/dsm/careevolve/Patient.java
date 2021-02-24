package org.broadinstitute.dsm.careevolve;

import static org.broadinstitute.dsm.careevolve.Covid19OrderRegistrar.ACTIVITIES_FIELD;
import static org.broadinstitute.dsm.careevolve.Covid19OrderRegistrar.ADDRESS_FIELD;
import static org.broadinstitute.dsm.careevolve.Covid19OrderRegistrar.ANSWER_FIELD;
import static org.broadinstitute.dsm.careevolve.Covid19OrderRegistrar.FIRST_NAME_FIELD;
import static org.broadinstitute.dsm.careevolve.Covid19OrderRegistrar.GUID_FIELD;
import static org.broadinstitute.dsm.careevolve.Covid19OrderRegistrar.LAST_NAME_FIELD;
import static org.broadinstitute.dsm.careevolve.Covid19OrderRegistrar.PROFILE_FIELD;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.dsm.exception.CareEvolveException;

public class Patient {

    @SerializedName("PatientID")
    private String patientId;

    @SerializedName("LastName")
    private String lastName;

    @SerializedName("FirstName")
    private String firstName;

    // YYYY-MM-DD
    @SerializedName("DateOfBirth")
    private String dateOfBirth;

    // cdc 1000-9 values
    @SerializedName("Race")
    private String race;

    @SerializedName("Ethnicity")
    private String ethnicity;

    @SerializedName("Gender")
    private String gender;

    @SerializedName("Address")
    private Address address;

    public Patient(String patientId,
                   String firstName,
                   String lastName,
                   String dateOfBirth,
                   String race,
                   String ethnicity,
                   String gender,
                   Address address) {
        this.patientId = patientId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.dateOfBirth = dateOfBirth;
        this.race = race;
        this.ethnicity = ethnicity;
        this.gender = gender;
        this.address = address;
    }

    public String getPatientId() {
        return patientId;
    }

    public boolean hasFullName() {
        return StringUtils.isNotBlank(firstName) && StringUtils.isNotBlank(lastName);
    }

    public boolean hasDateOfBirth() {
        return StringUtils.isNotBlank(dateOfBirth);
    }

    public boolean hasAddress() {
        return address != null;
    }

    @Override
    public String toString() {
        return "Patient{" +
                "patientId='" + patientId + '\'' +
                ", lastName='" + lastName + '\'' +
                ", firstName='" + firstName + '\'' +
                ", dateOfBirth='" + dateOfBirth + '\'' +
                ", race='" + race + '\'' +
                ", ethnicity='" + ethnicity + '\'' +
                ", gender='" + gender + '\'' +
                ", address=" + address +
                '}';
    }
}
