package org.broadinstitute.dsm.model.elastic.filter.query;

import static org.junit.Assert.*;

import org.broadinstitute.dsm.model.elastic.filter.Operator;
import org.broadinstitute.dsm.model.elastic.filter.query.QueryBuilderFactory;
import org.broadinstitute.dsm.model.elastic.filter.query.QueryPayload;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.junit.Test;

public class QueryBuilderFactoryTest {


    @Test
    public void buildQueryBuilder() {
        QueryPayload payload = new QueryPayload("dsm.medicalRecord", "medicalRecordId", new Integer[] {10});
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