package org.broadinstitute.dsm.model.elastic.export;

import org.apache.commons.lang3.StringUtils;

public class RequestPayload {

    private String index;
    private String id;

    public RequestPayload(String index, String id) {
        this.index = index;
        this.id = id;
    }

    public RequestPayload(String index) {
        this(index, StringUtils.EMPTY);
    }

    public String getIndex() {
        return index;
    }

    public String getId() {
        return id;
    }
}
