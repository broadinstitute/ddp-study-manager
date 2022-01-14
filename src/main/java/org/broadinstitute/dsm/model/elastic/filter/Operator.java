package org.broadinstitute.dsm.model.elastic.filter;

import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.dsm.model.Filter;

public enum Operator {
    LIKE(Filter.LIKE_TRIMMED),
    EQUALS(Filter.EQUALS_TRIMMED),
    GREATER_THAN_EQUALS(Filter.LARGER_EQUALS_TRIMMED),
    LESS_THAN_EQUALS(Filter.SMALLER_EQUALS_TRIMMED),
    IS_NOT_NULL(Filter.IS_NOT_NULL_TRIMMED),
    DIAMOND_EQUALS(Filter.DIAMOND_EQUALS),
    MULTIPLE_OPTIONS(Filter.OPEN_PARENTHESIS),
    STR_DATE(Filter.DATE_FORMAT),
    DATE_GREATER(Filter.DATE_GREATER),
    DATE_LESS(Filter.DATE_LESS),
    JSON_EXTRACT(Filter.JSON_EXTRACT),
    DATE(Filter.DATE);

    public static final String UNKNOWN_OPERATOR = "Unknown operator";

    private String value;

    Operator(String value) {
        this.value = value;
    }

    public static Operator getOperator(String value) {
        for (Operator op: Operator.values()) {
            if (op.value.equals(value)) return op;
        }
        throw new IllegalArgumentException(UNKNOWN_OPERATOR);
    }

    public static Operator extract(String filter) {
        String[] splittedFilter = filter.split(Filter.SPACE);
        if (isMultipleOptions(splittedFilter))
            return MULTIPLE_OPTIONS;
        String operator = Arrays.stream(splittedFilter)
                .filter(StringUtils::isNotBlank)
                .filter(str -> !Filter.OPEN_PARENTHESIS.equals(str) && !Filter.CLOSE_PARENTHESIS.equals(str))
                .filter(str -> Arrays.stream(Operator.values()).anyMatch(op -> op.value.equals(str)))
                .findFirst()
                .orElse(StringUtils.EMPTY);
        return getOperator(operator);
    }

    private static boolean isMultipleOptions(String[] splittedFilter) {
        return splittedFilter.length > 0 && Filter.OPEN_PARENTHESIS.equals(splittedFilter[0]) &&
                Filter.CLOSE_PARENTHESIS.equals(splittedFilter[splittedFilter.length - 1]);
    }
}
