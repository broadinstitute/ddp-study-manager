package org.broadinstitute.dsm.model.elastic.export;


import org.broadinstitute.dsm.util.ElasticSearchUtil;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

public class ElasticExportAdapter extends BaseExporter {

    @Override
    public void export(Map<String, Object> data) {
        RestHighLevelClient clientInstance = ElasticSearchUtil.getClientInstance();
        UpdateRequest updateRequest = Objects.requireNonNull(updateRequestPayload).getUpdateRequest(data);
        try {
            clientInstance.update(updateRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new RuntimeException("Error while exporting data to ES", e);
        }
    }
}
