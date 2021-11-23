package org.broadinstitute.dsm.model.elastic.export;

public class RequestPayload {

    private String index;
    private String id;

    public RequestPayload(String index, String id) {
        this.index = index;
        this.id = id;
    }

    public RequestPayload(String index) {
        this(index, "");
    }

    public String getIndex() {
        return index;
    }

    public String getId() {
        return id;
    }
}
