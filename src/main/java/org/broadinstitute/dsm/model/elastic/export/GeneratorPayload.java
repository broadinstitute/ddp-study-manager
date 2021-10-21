package org.broadinstitute.dsm.model.elastic.export;

import org.broadinstitute.dsm.model.NameValue;

public class GeneratorPayload {

    NameValue nameValue;
    long recordId;

    public GeneratorPayload(NameValue nameValue, long recordId) {
        this.nameValue = nameValue;
        this.recordId = recordId;
    }

    public NameValue getNameValue() {
        return nameValue;
    }

    public long getRecordId() {
        return recordId;
    }
}
