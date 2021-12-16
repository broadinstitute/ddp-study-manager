package org.broadinstitute.dsm.model.elastic.filter;

import org.elasticsearch.index.query.ExistsQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.RangeQueryBuilder;

public class QueryBuilderFactory {
    public static QueryBuilder buildQueryBuilder(Operator operator, QueryPayload payload) {
        QueryBuilder queryBuilder;
        switch (operator) {
            case LIKE:
            case EQUALS:
                queryBuilder = new MatchQueryBuilder(payload.getFieldName(), payload.getValue());
                break;
            case GREATER_THAN_EQUALS:
                RangeQueryBuilder greaterRangeQuery = new RangeQueryBuilder(payload.getFieldName());
                greaterRangeQuery.gte(payload.getValue());
                queryBuilder = greaterRangeQuery;
                break;
            case LESS_THAN_EQUALS:
                RangeQueryBuilder lessRangeQuery = new RangeQueryBuilder(payload.getFieldName());
                lessRangeQuery.lte(payload.getValue());
                queryBuilder = lessRangeQuery;
                break;
            case IS_NOT_NULL:
                queryBuilder = new ExistsQueryBuilder(payload.getFieldName());
                break;
            default:
                throw new IllegalArgumentException("Unknown operator");
        }
        return queryBuilder;
    }
}
