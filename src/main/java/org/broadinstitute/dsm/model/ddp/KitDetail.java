package org.broadinstitute.dsm.model.ddp;

import lombok.Data;

@Data
public class KitDetail {

    private String participantId;
    private String kitRequestId;
    private String kitType;
    private boolean needsApproval;
}
