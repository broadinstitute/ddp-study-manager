package org.broadinstitute.dsm.model.elastic.export;

import java.io.IOException;
import java.util.Map;

import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ElasticDataExportAdapter extends BaseExporter {

    private static final Logger logger = LoggerFactory.getLogger(ElasticDataExportAdapter.class);

    private UpsertDataRequestPayload upsertDataRequestPayload;
    private Map<String, Object> data;

    public ElasticDataExportAdapter(UpsertDataRequestPayload upsertDataRequestPayload, Map<String, Object> data) {
        this.upsertDataRequestPayload = upsertDataRequestPayload;
        this.data = data;
    }


    @Override
    public void export() {
        logger.info("initialize exporting data to ES");
        UpdateRequest updateRequest = upsertDataRequestPayload.getUpdateRequest(data);
        try {
            clientInstance.update(updateRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new RuntimeException("Error occurred while exporting data to ES", e);
        }
        logger.info("successfully exported data to ES");
    }
}
