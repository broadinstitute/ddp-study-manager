package org.broadinstitute.dsm.model.elastic.filter;

public class SingleQueryBuilder extends DsmAbstractQueryBuilder {

    public SingleQueryBuilder() {}

    @Override
    protected void buildEachQuery(FilterStrategy filterStrategy) {
        filterStrategy.build(boolQueryBuilder, queryBuilder);
    }
}
