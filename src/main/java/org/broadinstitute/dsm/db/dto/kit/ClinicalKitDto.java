package org.broadinstitute.dsm.db.dto.kit;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class ClinicalKitDto {

    @SerializedName ("participant_id")
    String collaboratorParticipantId;

    @SerializedName ("sample_id")
    String sampleId;

    @SerializedName ("sample_collection")
    String sampleCollection;

    @SerializedName ("material_type")
    String materialType;

    @SerializedName ("vessel_type")
    String vesselType;

    @SerializedName ("first_name")
    String firstName;

    @SerializedName ("last_name")
    String lastName;

    @SerializedName ("date_of_birth")
    String dateOfBirth;

    @SerializedName("sample_type")
    String sampleType;

    @SerializedName("gender")
    String gender;

    @SerializedName("accession_number")
    String accessionNumber;

    @SerializedName("collection_date")
    String collectionDate;

    @SerializedName("kit_label")
    String mfBarcode;

    public ClinicalKitDto(){}

    public void setSampleType(String kitType){
        switch (kitType.toLowerCase()){
            case "saliva":
                this.sampleType = "Normal";
                break;
            case "blood":
                this.sampleType = "N/A";
                break;
            default: //tissue
                this.sampleType = "Tumor";
                break;
        }
    }

    public void setGender(String genderString){
        switch (genderString.toLowerCase()){
            case "male":
                this.gender = "M";
                break;
            case "female":
                this.gender = "F";
                break;
            default: //intersex or prefer_not_answer
                this.gender = "U";
                break;
        }
    }

}
