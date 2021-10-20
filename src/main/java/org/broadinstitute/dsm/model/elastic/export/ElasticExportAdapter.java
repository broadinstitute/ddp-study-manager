package org.broadinstitute.dsm.model.elastic.export;


import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.indices.PutMappingRequest;

import java.io.IOException;
import java.util.Map;

public class ElasticExportAdapter extends BaseExporter {

    @Override
    public void exportData(Map<String, Object> data) {
        UpdateRequest updateRequest = upsertDataRequestPayload.getUpdateRequest(data);
        try {
            clientInstance.update(updateRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new RuntimeException("Error while exporting data to ES", e);
        }
    }

    @Override
    public void exportMapping(Map<String, Object> mapping) {
        PutMappingRequest putMappingRequest = upsertMappingRequestPayload.getPutMappingRequest();
        putMappingRequest.source(mapping);
        try {
            clientInstance.indices().putMapping(putMappingRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new RuntimeException("Error while updating mapping to ES", e);
        }
    }
}
