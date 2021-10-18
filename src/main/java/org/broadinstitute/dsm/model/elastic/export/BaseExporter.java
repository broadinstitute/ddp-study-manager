package org.broadinstitute.dsm.model.elastic.export;

public abstract class BaseExporter implements Exportable {

    protected UpdateRequestPayload updateRequestPayload;

    public void setUpdateRequestPayload(UpdateRequestPayload updateRequestPayload) {
        this.updateRequestPayload = updateRequestPayload;
    }
}
