package org.broadinstitute.dsm.model.elastic.export;


import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

public class ElasticExportAdapter extends BaseExporter {

    @Override
    public void exportData(Map<String, Object> data) {
        UpdateRequest updateRequest = Objects.requireNonNull(upsertDataRequestPayload).getUpdateRequest(data);
        try {
            clientInstance.update(updateRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new RuntimeException("Error while exporting data to ES", e);
        }
    }

    @Override
    public void exportMapping(Map<String, Object> mapping) {

    }
}
