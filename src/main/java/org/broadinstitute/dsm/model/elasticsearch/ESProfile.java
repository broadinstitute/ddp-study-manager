package org.broadinstitute.dsm.model.elasticsearch;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import org.broadinstitute.dsm.statics.ESObjectConstants;

@Data
public class ESProfile {

    @SerializedName(ESObjectConstants.FIRST_NAME)
    private String firstName;

    @SerializedName(ESObjectConstants.LAST_NAME)
    private String lastName;

    @SerializedName(ESObjectConstants.GUID)
    private String participantGuid;

    @SerializedName(ESObjectConstants.EMAIL)
    private String email;

}
