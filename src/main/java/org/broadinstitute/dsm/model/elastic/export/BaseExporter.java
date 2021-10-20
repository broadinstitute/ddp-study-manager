package org.broadinstitute.dsm.model.elastic.export;

import org.broadinstitute.dsm.util.ElasticSearchUtil;
import org.elasticsearch.client.RestHighLevelClient;

public abstract class BaseExporter implements Exportable {

    protected RestHighLevelClient clientInstance = ElasticSearchUtil.getClientInstance();

    protected UpsertDataRequestPayload upsertDataRequestPayload;

    public void setUpdateRequestPayload(UpsertDataRequestPayload upsertDataRequestPayload) {
        this.upsertDataRequestPayload = upsertDataRequestPayload;
    }
}
