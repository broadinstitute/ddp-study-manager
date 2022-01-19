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

public class UpsertPainless implements Exportable {

    private Generator generator;
    private RequestPayload requestPayload; // index, docId
    private ScriptBuilder scriptBuilder;

    public UpsertPainless(Generator generator, RequestPayload requestPayload, ScriptBuilder scriptBuilder) {
        this.generator = generator;
        this.requestPayload = requestPayload;
        this.scriptBuilder = scriptBuilder;
    }

    @Override
    public void export() {
        RestHighLevelClient clientInstance = ElasticSearchUtil.getClientInstance();
        UpdateRequest updateRequest = new UpdateRequest(requestPayload.getIndex(), Util.DOC, requestPayload.getDocId());
        Script painless = new Script(ScriptType.INLINE, "painless", scriptBuilder.build(), generator.generate());
        updateRequest.script(painless);
        try {
            clientInstance.update(updateRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new RuntimeException("Error occurred while exporting data to ES", e);
        }
    }

}
