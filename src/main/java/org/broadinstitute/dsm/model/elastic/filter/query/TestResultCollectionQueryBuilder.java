package org.broadinstitute.dsm.model.elastic.filter.query;

import org.broadinstitute.dsm.model.elastic.filter.Operator;
import org.broadinstitute.dsm.model.elastic.filter.splitter.BaseSplitter;
import org.broadinstitute.dsm.statics.DBConstants;
import org.elasticsearch.index.query.QueryBuilder;

public class TestResultCollectionQueryBuilder extends CollectionQueryBuilder {

    @Override
    public QueryBuilder buildEachQuery(Operator operator, QueryPayload queryPayload, BaseSplitter splitter) {
        queryPayload.setPath(String.join(DBConstants.ALIAS_DELIMITER, queryPayload.getPath(), splitter.getField()));
        return super.buildEachQuery(operator, queryPayload, splitter);
    }
}
