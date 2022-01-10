package org.broadinstitute.dsm.model.elastic.filter;

import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.index.query.NestedQueryBuilder;

public class CollectionQueryBuilder extends DsmAbstractQueryBuilder {

    public CollectionQueryBuilder() {}

    @Override
    protected void buildEachQuery(FilterStrategy filterStrategy) {
        filterStrategy.build(boolQueryBuilder, new NestedQueryBuilder(buildPath(), queryBuilder, ScoreMode.Avg));
    }

}

