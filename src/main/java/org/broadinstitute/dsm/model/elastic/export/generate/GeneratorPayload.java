package org.broadinstitute.dsm.model.elastic.export.generate;

import org.broadinstitute.dsm.model.NameValue;
import org.broadinstitute.dsm.model.elastic.Util;

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

    public String getName() {return nameValue.getName();}

    public Object getValue() { return nameValue.getValue(); }

    public int getRecordId() {
        return recordId;
    }

    public String getFieldName() {
        return Util.underscoresToCamelCase(Util.getDBElement(getName()).getColumnName());
    }
}
