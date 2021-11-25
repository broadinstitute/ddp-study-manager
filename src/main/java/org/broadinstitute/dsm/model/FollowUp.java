package org.broadinstitute.dsm.model;

import lombok.Data;
import org.broadinstitute.dsm.db.structure.ColumnName;

@Data
public class FollowUp {

    @ColumnName("fReceived")
    public String fReceived;

    @ColumnName("fRequest1")
    public String fRequest1;

    @ColumnName("fRequest2")
    public String fRequest2;

    @ColumnName("fRequest3")
    public String fRequest3;

    public FollowUp(String fRequest1, String fRequest2, String fRequest3, String fReceived) {
        this.fReceived = fReceived;
        this.fRequest1 = fRequest1;
        this.fRequest2 = fRequest2;
        this.fRequest3 = fRequest3;

    }
    public FollowUp() {

    }
}
