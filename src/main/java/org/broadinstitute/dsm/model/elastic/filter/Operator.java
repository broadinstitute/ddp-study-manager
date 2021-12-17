package org.broadinstitute.dsm.model.elastic.filter;

import org.broadinstitute.dsm.model.Filter;

public enum Operator {
    LIKE(Filter.LIKE_TRIMMED),
    EQUALS(Filter.EQUALS_TRIMMED),
    GREATER_THAN_EQUALS(Filter.LARGER_EQUALS_TRIMMED),
    LESS_THAN_EQUALS(Filter.SMALLER_EQUALS_TRIMMED),
    IS_NOT_NULL(Filter.IS_NOT_NULL_TRIMMED),
    DIAMOND_EQUALS(Filter.DIAMOND_EQUALS),
    DIAMOND_EQUALS(Filter.MUL);

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
        String operator = filter.split(" ")[1];
        return getOperator(operator);
    }
}
