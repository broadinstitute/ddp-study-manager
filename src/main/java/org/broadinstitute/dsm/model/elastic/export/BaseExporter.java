package org.broadinstitute.dsm.model.elastic.export;

import org.broadinstitute.dsm.util.ElasticSearchUtil;
import org.elasticsearch.client.RestHighLevelClient;

import java.util.Map;
import java.util.Objects;

public abstract class BaseExporter implements Exportable, ExportableHelper {
    protected RestHighLevelClient clientInstance = ElasticSearchUtil.getClientInstance();

    protected RequestPayload requestPayload;
    protected Map<String, Object> source;

    @Override
    public void setRequestPayload(RequestPayload requestPayload) {
        this.requestPayload = requestPayload;
    }

    @Override
    public void setSource(Map<String, Object> source) {
        this.source = source;
    }

}
