package org.broadinstitute.dsm.model.elastic.filter;

import org.elasticsearch.index.query.MatchQueryBuilder;

public class SingleQueryBuilder extends DsmAbstractQueryBuilder {

    @Override
    protected void buildEachQuery(FilterStrategy filterStrategy) {
        filterStrategy.build(boolQueryBuilder, queryBuilder);
    }
}
