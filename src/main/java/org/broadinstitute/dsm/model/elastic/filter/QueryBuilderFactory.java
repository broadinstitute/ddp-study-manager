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
            case LESS_THAN_EQUALS:
                queryBuilder = new RangeQueryBuilder(payload.getFieldName());
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
