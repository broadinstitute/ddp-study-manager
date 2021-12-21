package org.broadinstitute.dsm.model.elastic.filter;

import org.broadinstitute.dsm.model.Filter;

public enum Operator {
    LIKE(Filter.LIKE_TRIMMED),
    EQUALS(Filter.EQUALS_TRIMMED),
    GREATER_THAN_EQUALS(Filter.LARGER_EQUALS_TRIMMED),
    LESS_THAN_EQUALS(Filter.SMALLER_EQUALS_TRIMMED),
    IS_NOT_NULL(Filter.IS_NOT_NULL_TRIMMED),
    DIAMOND_EQUALS(Filter.DIAMOND_EQUALS),
    MULTIPLE_OPTIONS(Filter.OPEN_PARENTHESIS),
    DATE(Filter.DATE_FORMAT),
    DATE_GREATER(Filter.DATE_GREATER),
    DATE_LESS(Filter.DATE_LESS),
    JSON_EXTRACT(Filter.JSON_EXTRACT);

    private String value;

    Operator(String value) {
        this.value = value;
    }

    public static Operator getOperator(String value) {
        for (Operator op: Operator.values()) {
            if (op.value.equals(value)) return op;
        }
        throw new IllegalArgumentException("Unknown operator");
    }

    public static Operator extract(String filter) {
        if (filter.contains(Filter.IS_NOT_NULL_TRIMMED))
            return IS_NOT_NULL;
        else if (filter.startsWith(Filter.OPEN_PARENTHESIS))
            return MULTIPLE_OPTIONS;
        else if (filter.startsWith(Filter.DATE_FORMAT))
            return DATE;
        else if (filter.contains(Filter.DATE_GREATER))
            return DATE_GREATER;
        else if (filter.contains(Filter.DATE_LESS))
            return DATE_LESS;
        else if (filter.contains(Filter.JSON_EXTRACT))
            return JSON_EXTRACT;
        String operator = filter.split(" ")[1];
        return getOperator(operator);
    }
}
