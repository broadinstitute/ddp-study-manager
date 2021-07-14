package org.broadinstitute.dsm.model;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class ClinicalKit {

    @SerializedName("participant_id")
    String participantId;

    @SerializedName("sample_id")
    String sampleId;

    @SerializedName("sample_collection")
    String sampleCollection;

    @SerializedName("material_type")
    String materialType;

    @SerializedName("vessel_type")
    String vesselType;

    @SerializedName("mail_to_name")
    String mailToName;

    @SerializedName("date_of_birth")
    String dateOfBirth;

    public ClinicalKit(String ddpParrticipantId,
                       String sampleId,
                       String sampleCollection,
                       String materialType,
                       String vesselType,
                       String mailToName,
                       String dateOfBirth){

        this.participantId = ddpParrticipantId;
        this.sampleId = sampleId;
        this.sampleCollection = sampleCollection;
        this.materialType = materialType;
        this.vesselType = vesselType;
        this.mailToName = mailToName;
        this.dateOfBirth = dateOfBirth;
    }
}
