package org.broadinstitute.dsm.model.elastic.export.generate;

import org.broadinstitute.dsm.model.NameValue;

public class GeneratorPayload {

    NameValue nameValue;
    int recordId;

    public GeneratorPayload(NameValue nameValue, int recordId) {
        this.nameValue = nameValue;
        this.recordId = recordId;
    }

    public GeneratorPayload(NameValue nameValue) {
        this.nameValue = nameValue;
    }

    public NameValue getNameValue() {
        return nameValue;
    }

    public int getRecordId() {
        return recordId;
    }
}
