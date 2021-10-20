package org.broadinstitute.dsm.model.elastic.export;

import java.util.Objects;

import org.broadinstitute.dsm.model.NameValue;

public class ExportFacadePayload {
    private String index;
    private String id;
    private NameValue nameValue;

    public ExportFacadePayload(String index, String id, NameValue nameValue) {
        this.index = Objects.requireNonNull(index);
        this.id = Objects.requireNonNull(id);
        this.nameValue = Objects.requireNonNull(nameValue);
    }

    public String getIndex() {
        return index;
    }

    public String getId() {
        return id;
    }

    public NameValue getNameValue() {
        return nameValue;
    }
}
