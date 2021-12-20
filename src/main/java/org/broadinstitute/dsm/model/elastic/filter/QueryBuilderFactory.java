package org.broadinstitute.dsm.model.elastic.filter;

import org.elasticsearch.index.query.*;

public class QueryBuilderFactory {
    public static void buildQueryBuilder(Operator operator, QueryPayload payload, BoolQueryBuilder boolQueryBuilder, FilterStrategy filterStrategy) {
        switch (operator) {
            case LIKE:
            case EQUALS:
                filterStrategy.build(boolQueryBuilder, new MatchQueryBuilder(payload.getFieldName(), payload.getValues()[0]));
                break;
            case GREATER_THAN_EQUALS:
                RangeQueryBuilder greaterRangeQuery = new RangeQueryBuilder(payload.getFieldName());
                greaterRangeQuery.gte(payload.getValues()[0]);
                filterStrategy.build(boolQueryBuilder, greaterRangeQuery);
                break;
            case LESS_THAN_EQUALS:
                RangeQueryBuilder lessRangeQuery = new RangeQueryBuilder(payload.getFieldName());
                lessRangeQuery.lte(payload.getValues()[0]);
                filterStrategy.build(boolQueryBuilder, lessRangeQuery);
                break;
            case IS_NOT_NULL:
                filterStrategy.build(boolQueryBuilder, new ExistsQueryBuilder(payload.getFieldName()));
                break;
            case MULTIPLE_OPTIONS:
                Object[] values = payload.getValues();
                for (Object value : values)
                    filterStrategy.build(boolQueryBuilder, new MatchQueryBuilder(payload.getFieldName(), value));
                break;
            default:
                throw new IllegalArgumentException("Unknown operator");
        }
    }
}
