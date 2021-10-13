package org.broadinstitute.dsm.db.dto.kit;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class ClinicalKitDto {

    private final String NORMAL = "Normal";
    private final String TUMOR = "Tumor";
    private final String NA = "NA";

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

    @SerializedName ("mail_to_name")
    String mailToName;

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

    String instanceName;

    public ClinicalKitDto(){}

    public void setSampleType(String kitType){
        switch (kitType.toLowerCase()){
            case "saliva":
                this.sampleType = NORMAL;
                break;
            case "blood":
                this.sampleType = NA;
                break;
            default: //tissue
                this.sampleType = TUMOR;
                break;
        }

    }

}
