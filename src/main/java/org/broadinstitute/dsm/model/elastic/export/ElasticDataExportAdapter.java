package org.broadinstitute.dsm.model.elastic.export;

import java.io.IOException;

import org.broadinstitute.dsm.model.elastic.Util;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.script.Script;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ElasticDataExportAdapter extends BaseExporter {

    private static final Logger logger = LoggerFactory.getLogger(ElasticDataExportAdapter.class);

    @Override
    public void export() {
        logger.info("initialize exporting data to ES");
        UpdateRequest updateRequest = new UpdateRequest(requestPayload.getIndex(), Util.DOC, requestPayload.getId())
                .doc(source)
                .retryOnConflict(5);
        try {
            clientInstance.update(updateRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new RuntimeException("Error occurred while exporting data to ES", e);
        }
        logger.info("successfully exported data to ES");
    }
}
