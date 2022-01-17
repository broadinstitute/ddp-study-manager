package org.broadinstitute.dsm.model.elastic.filter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.dsm.model.Filter;

public enum Operator {
    LIKE(Filter.LIKE_TRIMMED, "((?!').)*(LIKE)"),
    EQUALS(Filter.EQUALS_TRIMMED, "((?!(<|>|')).)*(=)"),
    GREATER_THAN_EQUALS(Filter.LARGER_EQUALS_TRIMMED, ""),
    LESS_THAN_EQUALS(Filter.SMALLER_EQUALS_TRIMMED, ""),
    IS_NOT_NULL(Filter.IS_NOT_NULL_TRIMMED, ""),
    DIAMOND_EQUALS(Filter.DIAMOND_EQUALS, ""),
    MULTIPLE_OPTIONS(Filter.OPEN_PARENTHESIS, ""),
    STR_DATE(Filter.DATE_FORMAT, ""),
    DATE_GREATER(Filter.DATE_GREATER, ""),
    DATE_LESS(Filter.DATE_LESS, ""),
    JSON_EXTRACT(Filter.JSON_EXTRACT, ""),
    DATE(Filter.DATE, "");

    public static final String UNKNOWN_OPERATOR = "Unknown operator";

    private String value;
    private Pattern regex;

    Operator(String value, String regex) {
        this.value = value;
        this.regex = Pattern.compile(regex);
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

    public static List<String> collectOperatorsFromFilter(String filter) {
        List<String> operators = new ArrayList<>();
        for (Operator operator : Operator.values()) {
            int startIndex = filter.indexOf(operator.value);
            if (startIndex == -1) continue;
            String extractedOperator = filter.substring(startIndex, startIndex + operator.value.length());
            operators.add(extractedOperator);
        }
        return operators;
    }

    public static Operator extractOperator(String filter) {
        String[] splittedFilter = filter.split(Filter.SPACE);
        Arrays.stream(splittedFilter)
                .filter(StringUtils::isNotBlank)
                .filter(word -> Arrays.stream(Operator.values()).anyMatch(op -> op.value.equals(word)))
                .distinct()
                .reduce((prev, curr) -> String.join(Filter.SPACE, prev, curr));

        Operator operator = null;
        for (Operator op: Operator.values()) {
            int startIndex = filter.indexOf(op.value);
            if (startIndex <= 0) continue;
            String extractedOperator = filter.substring(0, startIndex + op.value.length());
            boolean isOperatorMatch = op.regex.matcher(extractedOperator).matches();
            if (isOperatorMatch) {
                operator = op;
                break;
            }
        }
        if (Objects.isNull(operator)) throw new NoSuchElementException(UNKNOWN_OPERATOR);
        return operator;
    }
}
