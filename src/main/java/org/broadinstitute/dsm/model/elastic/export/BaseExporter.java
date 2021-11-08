package org.broadinstitute.dsm.model.elastic.export;

import org.broadinstitute.dsm.util.ElasticSearchUtil;
import org.elasticsearch.client.RestHighLevelClient;

import java.util.Objects;

public abstract class BaseExporter implements Exportable {
    protected RestHighLevelClient clientInstance = ElasticSearchUtil.getClientInstance();
}
