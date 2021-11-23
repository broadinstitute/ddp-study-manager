package org.broadinstitute.dsm.model.elastic.export;

import java.io.IOException;
import java.util.Map;

import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.indices.PutMappingRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ElasticMappingExportAdapter extends BaseExporter {

    private static final Logger logger = LoggerFactory.getLogger(ElasticMappingExportAdapter.class);

    private UpsertMappingRequestPayload upsertMappingRequestPayload;
    private Map<String, Object> mapping;

    public void setUpsertMappingRequestPayload(UpsertMappingRequestPayload upsertMappingRequestPayload) {
        this.upsertMappingRequestPayload = upsertMappingRequestPayload;
    }

    public void setMapping(Map<String, Object> mapping) {
        this.mapping = mapping;
    }

    @Override
    public void export() {
        logger.info("initialize exporting mapping to ES");
        PutMappingRequest putMappingRequest = upsertMappingRequestPayload.getPutMappingRequest();
        putMappingRequest.source(mapping);
        try {
            clientInstance.indices().putMapping(putMappingRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new RuntimeException("Error occurred while updating mapping to ES", e);
        }
        logger.info("successfully exported mapping to ES");
    }
}
