package org.broadinstitute.dsm.model.birch;

import lombok.Data;

@Data
public class TestBostonResult {
    private String orderMessageId;
    private String institutionId;
    private String sampleId;
    private String result;
    private String reason;
    private String timeCompleted;
    
}
