package org.broadinstitute.dsm.model.elastic.filter.query;

import org.broadinstitute.dsm.model.elastic.filter.FilterStrategy;
import org.broadinstitute.dsm.model.elastic.filter.Operator;
import org.broadinstitute.dsm.model.elastic.filter.splitter.BaseSplitter;
import org.elasticsearch.index.query.QueryBuilder;

public class SingleQueryBuilder extends BaseQueryBuilder {


    @Override
    protected QueryBuilder build(QueryBuilder queryBuilder) {
        return queryBuilder;
    }

    @Override
    protected QueryBuilder buildEachQuery(Operator operator, QueryPayload queryPayload, BaseSplitter splitter) {
        return null;
    }
}
