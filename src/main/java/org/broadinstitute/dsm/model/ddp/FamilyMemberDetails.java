package org.broadinstitute.dsm.model.ddp;

import com.google.gson.annotations.SerializedName;

public class FamilyMemberDetails {

    @SerializedName(value = "DATSTAT_FIRSTNAME", alternate = "firstName")
    private String firstName;

    @SerializedName(value = "DATSTAT_LASTNAME", alternate = "lastName")
    private String lastName;

    @SerializedName(value = "MEMBER_TYPE", alternate = "memberType")
    private String memberType;

    @SerializedName(value = "FAMILY_ID", alternate = "familyId")
    private Integer familyId;

    @SerializedName(value = "COLLABORATOR_PARTICIPANT_ID", alternate = "collaboratorParticipantId")
    private String collaboratorParticipantId;

}
