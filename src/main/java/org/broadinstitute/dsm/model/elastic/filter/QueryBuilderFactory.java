package org.broadinstitute.dsm.model.elastic.filter;

import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.ExistsQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.RangeQueryBuilder;

public class QueryBuilderFactory {
    public static QueryBuilder buildQueryBuilder(Operator operator, QueryPayload payload) {
        QueryBuilder qb;
        switch (operator) {
            case LIKE:
            case EQUALS:
            case DATE:
                qb = new MatchQueryBuilder(payload.getFieldName(), payload.getValues()[0]);
                break;
            case GREATER_THAN_EQUALS:
                RangeQueryBuilder greaterRangeQuery = new RangeQueryBuilder(payload.getFieldName());
                greaterRangeQuery.gte(payload.getValues()[0]);
                qb = greaterRangeQuery;
                break;
            case LESS_THAN_EQUALS:
                RangeQueryBuilder lessRangeQuery = new RangeQueryBuilder(payload.getFieldName());
                lessRangeQuery.lte(payload.getValues()[0]);
                qb = lessRangeQuery;
                break;
            case DATE_GREATER:
                RangeQueryBuilder dateGreaterQuery = new RangeQueryBuilder(payload.getFieldName());
                dateGreaterQuery.gte(payload.getValues()[0]);
                qb = dateGreaterQuery;
                break;
            case DATE_LESS:
                RangeQueryBuilder dateLessQuery = new RangeQueryBuilder(payload.getFieldName());
                dateLessQuery.lte(payload.getValues()[0]);
                qb = dateLessQuery;
                break;
            case IS_NOT_NULL:
                qb = new ExistsQueryBuilder(payload.getFieldName());
                break;
            case MULTIPLE_OPTIONS:
                BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
                Object[] values = payload.getValues();
                for (Object value : values) {
                    boolQueryBuilder.should(new MatchQueryBuilder(payload.getFieldName(), value));
                }
                qb = boolQueryBuilder;
                break;
            case JSON_EXTRACT:
                Object[] dynamicFieldValues = payload.getValues();
                if (!StringUtils.EMPTY.equals(dynamicFieldValues[0])) {
                    qb = new MatchQueryBuilder(payload.getFieldName(), dynamicFieldValues[0]);
                } else {
                    qb = new ExistsQueryBuilder(payload.getFieldName());
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown operator");
        }
        return qb;
    }
}
