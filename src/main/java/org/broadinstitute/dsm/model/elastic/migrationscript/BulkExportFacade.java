package org.broadinstitute.dsm.model.elastic.migrationscript;

import org.broadinstitute.dsm.util.ElasticSearchUtil;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;

import java.io.IOException;
import java.util.Map;

import static org.broadinstitute.dsm.model.elastic.Util.DOC;

public class BulkExportFacade {

    private String index;
    private BulkRequest bulkRequest;

    public BulkExportFacade(String index) {
        this.index = index;
        this.bulkRequest = new BulkRequest();
    }

    public void addDataToRequest(Map mapToUpsert, String docId) {
        bulkRequest.add(createRequest(mapToUpsert, docId));
    }

    private UpdateRequest createRequest(Map mapToUpsert, String docId) {
        UpdateRequest updateRequest = new UpdateRequest(index, DOC, docId);
        updateRequest.doc(mapToUpsert);
        return updateRequest;
    }

    public void executeBulkUpsert() {
        RestHighLevelClient client = ElasticSearchUtil.getClientInstance();
        try {
            if (bulkRequest.requests().size() > 0) {
                client.bulk(bulkRequest, RequestOptions.DEFAULT);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }



}
