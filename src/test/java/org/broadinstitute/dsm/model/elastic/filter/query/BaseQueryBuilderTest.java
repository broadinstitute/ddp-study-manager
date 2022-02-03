package org.broadinstitute.dsm.model.elastic.filter.query;

import static org.junit.Assert.*;

import org.broadinstitute.dsm.model.elastic.filter.Operator;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.junit.Test;

public class BaseQueryBuilderTest {


    @Test
    public void buildQueryBuilder() {
        QueryPayload payload = new QueryPayload("dsm.medicalRecord", "medicalRecordId", new Integer[] {10});
        Operator operator = Operator.EQUALS;
        QueryBuilder queryBuilder = BaseQueryBuilder.buildQueryBuilder();
        assertTrue(queryBuilder instanceof MatchQueryBuilder);
        operator = Operator.LIKE;
        queryBuilder = BaseQueryBuilder.buildQueryBuilder();
        assertTrue(queryBuilder instanceof MatchQueryBuilder);
        operator = Operator.GREATER_THAN_EQUALS;
        queryBuilder = BaseQueryBuilder.buildQueryBuilder();
        assertTrue(queryBuilder instanceof RangeQueryBuilder);
    }

}