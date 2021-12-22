package org.broadinstitute.dsm.model.elastic.filter;

import org.broadinstitute.dsm.model.elastic.export.parse.Parser;
import org.elasticsearch.index.query.MatchQueryBuilder;

public class SingleQueryBuilder extends DsmAbstractQueryBuilder {

    public SingleQueryBuilder(String filter, Parser parser) {
        super(filter, parser);
    }

    public SingleQueryBuilder() {}

    @Override
    protected void buildEachQuery(FilterStrategy filterStrategy) {
        filterStrategy.build(boolQueryBuilder, queryBuilder);
    }
}
