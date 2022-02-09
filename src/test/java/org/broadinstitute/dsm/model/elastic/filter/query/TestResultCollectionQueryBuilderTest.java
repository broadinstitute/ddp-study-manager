package org.broadinstitute.dsm.model.elastic.filter.query;

import org.broadinstitute.dsm.model.elastic.filter.FilterParser;
import org.broadinstitute.dsm.model.elastic.filter.Operator;
import org.broadinstitute.dsm.model.elastic.filter.splitter.JsonContainsSplitter;
import org.elasticsearch.index.query.QueryBuilder;
import org.junit.Test;

public class TestResultCollectionQueryBuilderTest {


    @Test
    public void build() {
        BaseQueryBuilder queryBuilder = new TestResultCollectionQueryBuilder();
        QueryPayload queryPayload =
                new QueryPayload("dsm.kitRequestShipping", "testResult.isCorrected", new FilterParser().parse(new String[] {"'true'"}));
        QueryBuilder query = queryBuilder.buildEachQuery(Operator.JSON_CONTAINS, queryPayload, new JsonContainsSplitter());

    }

}
