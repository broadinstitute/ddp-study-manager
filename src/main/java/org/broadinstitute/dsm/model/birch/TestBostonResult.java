package org.broadinstitute.dsm.model.birch;

import com.google.gson.annotations.SerializedName;
import lombok.Data;


@Data
public class TestBostonResult {
    private String orderMessageId;
    private String institutionId;

    @SerializedName("sample_id")
    private String sampleId;
    private String result;
    private String reason;
    @SerializedName ("time_completed")
    private String timeCompleted;
    @SerializedName ("is_corrected")
    private boolean isCorrected;
    private String kitRequestId;
    
}
