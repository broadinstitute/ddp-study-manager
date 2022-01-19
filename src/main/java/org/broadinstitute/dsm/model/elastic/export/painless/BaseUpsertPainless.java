package org.broadinstitute.dsm.model.elastic.export.painless;

import org.broadinstitute.dsm.model.elastic.export.BaseExporter;
import org.broadinstitute.dsm.model.elastic.export.Exportable;
import org.broadinstitute.dsm.model.elastic.export.RequestPayload;

import java.util.Map;

public abstract class BaseUpsertPainless implements Exportable {

    protected String script;


    private Map<String, Object> source;
    private RequestPayload requestPayload; // index, docId

    public BaseUpsertPainless(String script, Object source, RequestPayload requestPayload) {
        this.script = script;
//        this.source = source;
        this.requestPayload = requestPayload;
    }

    @Override
    public void export() {

    }

}
