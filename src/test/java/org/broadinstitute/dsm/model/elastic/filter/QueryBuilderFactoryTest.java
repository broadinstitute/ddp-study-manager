package org.broadinstitute.dsm.model.elastic.filter;

import static org.junit.Assert.*;

import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.junit.Test;

public class QueryBuilderFactoryTest {


    @Test
    public void buildQueryBuilder() {
        QueryPayload payload = new QueryPayload("medicalRecordId", 15);
        Operator operator = Operator.EQUALS;
        QueryBuilder queryBuilder = QueryBuilderFactory.buildQueryBuilder(operator, payload);
        assertTrue(queryBuilder instanceof MatchQueryBuilder);
        operator = Operator.LIKE;
        queryBuilder = QueryBuilderFactory.buildQueryBuilder(operator, payload);
        assertTrue(queryBuilder instanceof MatchQueryBuilder);
        operator = Operator.GREATER_THAN_EQUALS;
        queryBuilder = QueryBuilderFactory.buildQueryBuilder(operator, payload);
        assertTrue(queryBuilder instanceof RangeQueryBuilder);
    }

}