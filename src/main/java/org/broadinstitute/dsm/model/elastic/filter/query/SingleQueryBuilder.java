package org.broadinstitute.dsm.model.elastic.filter.query;

import org.broadinstitute.dsm.model.elastic.filter.FilterStrategy;

public class SingleQueryBuilder extends DsmAbstractQueryBuilder {

    public SingleQueryBuilder() {}

    @Override
    protected void buildEachQuery(FilterStrategy filterStrategy) {
        filterStrategy.build(boolQueryBuilder, queryBuilder);
    }
}
