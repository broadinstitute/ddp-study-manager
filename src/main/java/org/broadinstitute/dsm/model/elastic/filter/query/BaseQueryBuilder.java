package org.broadinstitute.dsm.model.elastic.filter.query;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.dsm.model.elastic.filter.Operator;
import org.broadinstitute.dsm.model.elastic.filter.splitter.BaseSplitter;
import org.broadinstitute.dsm.model.elastic.filter.splitter.GreaterThanEqualsSplitter;
import org.broadinstitute.dsm.model.elastic.filter.splitter.IsNullSplitter;
import org.broadinstitute.dsm.model.elastic.filter.splitter.JsonExtractSplitter;
import org.broadinstitute.dsm.model.elastic.filter.splitter.LessThanEqualsSplitter;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.ExistsQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.index.query.RegexpQueryBuilder;

public abstract class BaseQueryBuilder {

    protected Operator operator;
    protected QueryPayload payload;
    protected BaseSplitter splitter;

    protected QueryBuilder buildQueryBuilder() {
        QueryBuilder qb;
        switch (operator) {
            case LIKE:
            case EQUALS:
            case DATE:
            case DIAMOND_EQUALS:
            case STR_DATE:
                qb = build(new MatchQueryBuilder(payload.getFieldName(), payload.getValues()[0]));
                break;
            case GREATER_THAN_EQUALS:
                RangeQueryBuilder greaterRangeQuery = new RangeQueryBuilder(payload.getFieldName());
                greaterRangeQuery.gte(payload.getValues()[0]);
                qb = build(greaterRangeQuery);
                break;
            case LESS_THAN_EQUALS:
                RangeQueryBuilder lessRangeQuery = new RangeQueryBuilder(payload.getFieldName());
                lessRangeQuery.lte(payload.getValues()[0]);
                qb = build(lessRangeQuery);
                break;
            case DATE_GREATER:
                RangeQueryBuilder dateGreaterQuery = new RangeQueryBuilder(payload.getFieldName());
                dateGreaterQuery.gte(payload.getValues()[0]);
                qb = build(dateGreaterQuery);
                break;
            case DATE_LESS:
                RangeQueryBuilder dateLessQuery = new RangeQueryBuilder(payload.getFieldName());
                dateLessQuery.lte(payload.getValues()[0]);
                qb = build(dateLessQuery);
                break;
            case IS_NOT_NULL:
                qb = buildIsNotNullAndEmpty();
                break;
            case IS_NULL:
                qb = buildIsNullQuery();
                break;
            case MULTIPLE_OPTIONS:
                BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
                Object[] values = payload.getValues();
                for (Object value : values) {
                    boolQueryBuilder.should(new MatchQueryBuilder(payload.getFieldName(), value));
                }
                qb = build(boolQueryBuilder);
                break;
            case JSON_EXTRACT:
                Object[] dynamicFieldValues = payload.getValues();
                JsonExtractSplitter jsonExtractSplitter = (JsonExtractSplitter) splitter;
//                buildQueryBuilder(jsonExtractSplitter.getOperator());
                if (!StringUtils.EMPTY.equals(dynamicFieldValues[0])) {
                    if (jsonExtractSplitter.getDecoratedSplitter() instanceof GreaterThanEqualsSplitter) {
                        qb = new RangeQueryBuilder(payload.getFieldName());
                        ((RangeQueryBuilder)qb).gte(dynamicFieldValues[0]);
                    } else if (jsonExtractSplitter.getDecoratedSplitter() instanceof LessThanEqualsSplitter) {
                        qb = new RangeQueryBuilder(payload.getFieldName());
                        ((RangeQueryBuilder)qb).lte(dynamicFieldValues[0]);
                    } else {
                        qb = new MatchQueryBuilder(payload.getFieldName(), dynamicFieldValues[0]);
                    }
                } else {
                    if (jsonExtractSplitter.getDecoratedSplitter() instanceof IsNullSplitter) {
                        qb = buildIsNullQuery();
                    } else {
                        qb = buildIsNotNullAndEmpty();
                    }
                }
                qb = build(qb);
                break;
            default:
                throw new IllegalArgumentException(Operator.UNKNOWN_OPERATOR);
        }
        return qb;
    }

    private QueryBuilder buildIsNotNullAndEmpty() {
        BoolQueryBuilder isNotNullAndNotEmpty = new BoolQueryBuilder();
        isNotNullAndNotEmpty.must(new ExistsQueryBuilder(payload.getFieldName()));
        isNotNullAndNotEmpty.must(new RegexpQueryBuilder(payload.getFieldName(), DsmAbstractQueryBuilder.ONE_OR_MORE_REGEX));
        return isNotNullAndNotEmpty;
    }

    protected abstract QueryBuilder build(QueryBuilder queryBuilder);

    private QueryBuilder buildIsNullQuery() {
        BoolQueryBuilder existsWithEmpty = new BoolQueryBuilder();
        existsWithEmpty.mustNot(build(new ExistsQueryBuilder(payload.getFieldName())));
        return existsWithEmpty;
    }

    protected abstract QueryBuilder buildEachQuery(Operator operator,
                                                   QueryPayload queryPayload,
                                                   BaseSplitter splitter);

}
