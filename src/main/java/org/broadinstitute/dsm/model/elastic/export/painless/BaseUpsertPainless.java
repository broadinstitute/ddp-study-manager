package org.broadinstitute.dsm.model.elastic.export.painless;

import java.io.IOException;

import org.broadinstitute.dsm.model.elastic.Util;
import org.broadinstitute.dsm.model.elastic.export.Exportable;
import org.broadinstitute.dsm.model.elastic.export.RequestPayload;
import org.broadinstitute.dsm.model.elastic.export.generate.Generator;
import org.broadinstitute.dsm.util.ElasticSearchUtil;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;

public abstract class BaseUpsertPainless implements Exportable {

    protected String script;
    private Generator generator;
    private RequestPayload requestPayload; // index, docId

    public BaseUpsertPainless(String script, Generator generator, RequestPayload requestPayload) {
        this.script = script;
        this.generator = generator;
        this.requestPayload = requestPayload;
    }

    @Override
    public void export() {
        RestHighLevelClient clientInstance = ElasticSearchUtil.getClientInstance();
        UpdateRequest updateRequest = new UpdateRequest(requestPayload.getIndex(), Util.DOC, requestPayload.getDocId());
        Script painless = new Script(ScriptType.INLINE, "painless", script, generator.generate());
        updateRequest.script(painless);
        try {
            clientInstance.update(updateRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new RuntimeException("Error occurred while exporting data to ES", e);
        }
    }

}
