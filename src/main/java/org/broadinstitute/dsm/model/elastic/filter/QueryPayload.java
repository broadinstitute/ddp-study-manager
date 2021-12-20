package org.broadinstitute.dsm.model.elastic.filter;

import lombok.Getter;

@Getter
public class QueryPayload {

    private String property;
    private String path;
    Object[] values;

    public QueryPayload(String path, String property, Object[] values) {
        this.path = path;
        this.property = property;
        this.values = values;
    }

    public String getFieldName() {
        return path + "." + property;
    }
}
