package org.broadinstitute.dsm.model.elastic.filter;

import org.broadinstitute.dsm.model.Filter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class AndOrFilterSeparator {

    public static final String DSM_ALIAS_REGEX = "(m|p|r|t|d|oD|o|k|JS|ST|DA|\\()(\\.|\\s)*([a-z]|O|R|T){1,2}";
    public static final String OR_DSM_ALIAS_REGEX = "(OR) " + DSM_ALIAS_REGEX;
    public static final String AND_DSM_ALIAS_REGEX = "(AND) " + DSM_ALIAS_REGEX;
    public static final int AND_PATTERN_MATCHER_NUMBER = 8;
    public static final int OR_PATTERN_MATCHER_NUMBER = 7;
    public static final int MINIMUM_STEP_FROM_OPERATOR = 3;

    private String filter;

    public AndOrFilterSeparator(String filter) {
        this.filter = filter;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

    protected Map<String, List<String>> parseFiltersByLogicalOperators() {
        Map<String, List<String>> filterByLogicalOperators = new ConcurrentHashMap<>(Map.of(Filter.AND_TRIMMED, new ArrayList<>(), Filter.OR_TRIMMED,
                new ArrayList<>()));
        int andIndex = filter.indexOf(Filter.AND_TRIMMED);
        int orIndex = filter.indexOf(Filter.OR_TRIMMED);
        while (andIndex != -1 || orIndex != -1) {
            andIndex = findProperOperatorSplitterIndex(Filter.AND_TRIMMED, andIndex, AND_PATTERN_MATCHER_NUMBER, MINIMUM_STEP_FROM_OPERATOR);
            orIndex = findProperOperatorSplitterIndex(Filter.OR_TRIMMED, orIndex, OR_PATTERN_MATCHER_NUMBER, MINIMUM_STEP_FROM_OPERATOR);
            if (andIndex != -1) {
                andIndex = getIndex(filterByLogicalOperators, andIndex, Filter.AND_TRIMMED);
            } else if (orIndex != -1){
                orIndex = getIndex(filterByLogicalOperators, orIndex, Filter.OR_TRIMMED);
            }
        }
        handleSpecialCases(filterByLogicalOperators);
        return filterByLogicalOperators;
    }

    private void handleSpecialCases(Map<String, List<String>> filterByLogicalOperators) {
        final String additionalValuesJsonIsNotNull = "additional_values_json IS NOT NULL";
        for (Map.Entry<String, List<String>> entry: filterByLogicalOperators.entrySet()) {
            List<String> filteredByNotAdditionalValuesIsNotNull = entry.getValue().stream()
                    .filter(f -> !f.contains(additionalValuesJsonIsNotNull))
                    .collect(Collectors.toList());
        filterByLogicalOperators.put(entry.getKey(), filteredByNotAdditionalValuesIsNotNull);
        }
    }

    /**
     * "AND ( oD.request = 'review' OR oD.request = 'no' ) " - filter
     *
     * @param operator -"AND", "OR"
     * @param startIndex - index where first operator is found
     * @param patternMatcherNumber - number to add start index to find out whether it matches alias regex | "AND ( o"
     * @param nextOperatorFromNumber - number to add start index to find next operator index | OR oD. index of OR
     * @return index of next proper operator after first operator, proper operator matches either AND_DSM_ALIAS_REGEX or OR_DSM_ALIAS_REGEX
     */
    private int findProperOperatorSplitterIndex(String operator, int startIndex, int patternMatcherNumber, int nextOperatorFromNumber) {
        String aliasRegex = getAliasRegexByOperator(operator);
        while (startIndex != -1 && !filter.substring(startIndex, startIndex + patternMatcherNumber).matches(aliasRegex)) {
            startIndex = findNextOperatorIndex(operator, startIndex + nextOperatorFromNumber);
        }
        return startIndex;
    }

    private String getAliasRegexByOperator(String operator) {
        return "AND".equals(operator) ? AND_DSM_ALIAS_REGEX : OR_DSM_ALIAS_REGEX;
    }

    private int findNextOperatorIndex(String operator, int fromIndex) {
        return filter.indexOf(operator, fromIndex);
    }

    private int getIndex(Map<String, List<String>> filterByLogicalOperators, int index, String operator) {
        boolean matches = filter.substring(index, getFilterIndex(index, getPatternMatcherNumberByOperator(operator))).matches(Pattern.compile(getAliasRegexByOperator(operator)).pattern());

        int orPrecedeIndex = findNextOperatorIndex(Filter.OR_TRIMMED, index + MINIMUM_STEP_FROM_OPERATOR);
        orPrecedeIndex = findProperOperatorSplitterIndex(Filter.OR_TRIMMED, orPrecedeIndex, OR_PATTERN_MATCHER_NUMBER, MINIMUM_STEP_FROM_OPERATOR);

        int andPrecedeIndex = findNextOperatorIndex(Filter.AND_TRIMMED, index + MINIMUM_STEP_FROM_OPERATOR);
        andPrecedeIndex = findProperOperatorSplitterIndex(Filter.AND_TRIMMED, andPrecedeIndex, AND_PATTERN_MATCHER_NUMBER, MINIMUM_STEP_FROM_OPERATOR);

        if (isLeftMostOR(matches, orPrecedeIndex, andPrecedeIndex)) {
            filterByLogicalOperators.get(operator).add(filter.substring(index + MINIMUM_STEP_FROM_OPERATOR, orPrecedeIndex == -1 ? andPrecedeIndex : orPrecedeIndex).trim());
            index = isAndOperator(operator) ? andPrecedeIndex : orPrecedeIndex;
        } else if (isLeftMostAND(matches, orPrecedeIndex, andPrecedeIndex)) {
            filterByLogicalOperators.get(operator).add(filter.substring(index + MINIMUM_STEP_FROM_OPERATOR, andPrecedeIndex == -1 ? orPrecedeIndex : andPrecedeIndex).trim());
            index = isAndOperator(operator) ? andPrecedeIndex : orPrecedeIndex;
        } else {
            filterByLogicalOperators.get(operator).add(filter.substring(index + MINIMUM_STEP_FROM_OPERATOR).trim());
            index = isAndOperator(operator) ? andPrecedeIndex : orPrecedeIndex;
        }
        return index;
    }

    private boolean isLeftMostOR(boolean matches, int orPrecedeIndex, int andPrecedeIndex) {
        return orPrecedeIndex < andPrecedeIndex && matches;
    }

    private boolean isLeftMostAND(boolean matches, int orPrecedeIndex, int andPrecedeIndex) {
        return andPrecedeIndex < orPrecedeIndex && matches;
    }

    private boolean isAndOperator(String operator) {
        return "AND".equals(operator);
    }

    private int getPatternMatcherNumberByOperator(String operator) {
        return isAndOperator(operator) ? AND_PATTERN_MATCHER_NUMBER : OR_PATTERN_MATCHER_NUMBER;
    }

    private int getFilterIndex(int andIndex, int patternMatcherNumber) {
        return andIndex + patternMatcherNumber;
    }

}
