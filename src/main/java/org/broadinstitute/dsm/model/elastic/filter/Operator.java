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
        filter = filter.trim();
        if (filter.endsWith(Filter.IS_NOT_NULL_TRIMMED) && !filter.startsWith(Filter.JSON_EXTRACT))
            return IS_NOT_NULL;
        else if (filter.startsWith(Filter.OPEN_PARENTHESIS) && filter.endsWith(Filter.CLOSE_PARENTHESIS))
            return MULTIPLE_OPTIONS;
        else if (filter.startsWith(Filter.DATE_FORMAT))
            return STR_DATE;
        else if (filter.startsWith(Filter.DATE))
            return DATE;
        else if (filter.contains(Filter.DATE_GREATER))
            return DATE_GREATER;
        else if (filter.contains(Filter.DATE_LESS))
            return DATE_LESS;
        else if (filter.contains(Filter.JSON_EXTRACT))
            return JSON_EXTRACT;
        String operator = Arrays.stream(filter.split(Filter.SPACE))
                .filter(StringUtils::isNotBlank)
                .filter(str -> Arrays.asList(Operator.values()).contains(str))
                .filter(str -> Arrays.stream(Operator.values()).anyMatch(op -> op.value.equals(str)))
                .findFirst()
                .orElse(StringUtils.EMPTY);
        return getOperator(operator);
    }
}
