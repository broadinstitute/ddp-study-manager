package org.broadinstitute.dsm.model.elastic.filter.query;

import org.broadinstitute.dsm.model.elastic.filter.Operator;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.NestedQueryBuilder;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class BaseQueryBuilderTest {


    @Test
    public void buildQueryBuilder() {
        QueryPayload payload = new QueryPayload("dsm.medicalRecord", "medicalRecordId", new Integer[] {10});
        Operator operator = Operator.EQUALS;

        CollectionQueryBuilder collectionQueryBuilder = new CollectionQueryBuilder();
        collectionQueryBuilder.operator = operator;
        collectionQueryBuilder.payload = payload;

        NestedQueryBuilder queryBuilder = (NestedQueryBuilder) collectionQueryBuilder.buildQueryBuilder();
        assertTrue(queryBuilder.query() instanceof MatchQueryBuilder);

        operator = Operator.LIKE;
        collectionQueryBuilder.operator = operator;

        queryBuilder = (NestedQueryBuilder) collectionQueryBuilder.buildQueryBuilder();
        assertTrue(queryBuilder.query() instanceof MatchQueryBuilder);


        operator = Operator.GREATER_THAN_EQUALS;
        collectionQueryBuilder.operator = operator;
        queryBuilder = (NestedQueryBuilder) collectionQueryBuilder.buildQueryBuilder();

        assertTrue(queryBuilder.query() instanceof RangeQueryBuilder);
    }

}