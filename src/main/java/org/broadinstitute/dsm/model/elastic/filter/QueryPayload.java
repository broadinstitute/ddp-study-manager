package org.broadinstitute.dsm.model.elastic.filter;

import lombok.Getter;

@Getter
public class QueryPayload {

    String fieldName;
    Object[] values;

    public QueryPayload(String fieldName, Object[] value) {
        this.fieldName = fieldName;
        this.values = value;
    }

    public QueryPayload(String fieldName) {
        this.fieldName = fieldName;
    }
}
