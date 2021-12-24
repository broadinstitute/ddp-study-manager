package org.broadinstitute.dsm.model.elastic.filter;

import org.broadinstitute.dsm.model.Filter;
import org.broadinstitute.dsm.statics.DBConstants;
import org.broadinstitute.dsm.statics.ESObjectConstants;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public class AndOrFilterSeparator {

    public static final String DSM_ALIAS_REGEX = "(m|p|r|t|d|oD|o|k|JS|\\()(\\.|\\s)*([a-z]|O)";
    public static final String OR_DSM_ALIAS_REGEX = "(OR) " + DSM_ALIAS_REGEX;
    public static final String AND_DSM_ALIAS_REGEX = "(AND) " + DSM_ALIAS_REGEX;
    public static final int AND_PATTERN_MATCHER_NUMBER = 7;
    public static final int OR_PATTERN_MATCHER_NUMBER = 6;
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

                int filterIndex = andIndex + AND_PATTERN_MATCHER_NUMBER;
                boolean matches =
                        filter.substring(andIndex, filterIndex).matches(Pattern.compile(AND_DSM_ALIAS_REGEX).pattern());

                int orPrecedeIndex = filter.indexOf(Filter.OR_TRIMMED, andIndex + MINIMUM_STEP_FROM_OPERATOR);
                while (orPrecedeIndex != -1 && !filter.substring(orPrecedeIndex, orPrecedeIndex + OR_PATTERN_MATCHER_NUMBER).matches(Pattern.compile(
                        OR_DSM_ALIAS_REGEX).pattern())) {
                    orPrecedeIndex = filter.indexOf(Filter.OR_TRIMMED, orPrecedeIndex + MINIMUM_STEP_FROM_OPERATOR);
                }

                int andPrecedeIndex = filter.indexOf(Filter.AND_TRIMMED, andIndex + MINIMUM_STEP_FROM_OPERATOR);
                while (andPrecedeIndex != -1 && !filter.substring(andPrecedeIndex, andPrecedeIndex + AND_PATTERN_MATCHER_NUMBER).matches(Pattern.compile(
                        AND_DSM_ALIAS_REGEX).pattern())) {
                    andPrecedeIndex = filter.indexOf(Filter.AND_TRIMMED, andPrecedeIndex + MINIMUM_STEP_FROM_OPERATOR);
                }

                if (orPrecedeIndex < andPrecedeIndex && matches) {
                    filterByLogicalOperators.get(Filter.AND_TRIMMED).add(filter.substring(andIndex + MINIMUM_STEP_FROM_OPERATOR, orPrecedeIndex == -1 ? andPrecedeIndex : orPrecedeIndex).trim());
                    andIndex = andPrecedeIndex;
                }
                else if (andPrecedeIndex < orPrecedeIndex && matches) {
                    filterByLogicalOperators.get(Filter.AND_TRIMMED).add(filter.substring(andIndex + MINIMUM_STEP_FROM_OPERATOR, andPrecedeIndex == -1 ? orPrecedeIndex : andPrecedeIndex).trim());
                    andIndex = andPrecedeIndex;
                } else {
                    filterByLogicalOperators.get(Filter.AND_TRIMMED).add(filter.substring(andIndex + MINIMUM_STEP_FROM_OPERATOR).trim());
                    andIndex = andPrecedeIndex;
                }
            } else {
                int filterIndex = orIndex + OR_PATTERN_MATCHER_NUMBER;
                boolean matches = filter.substring(orIndex, filterIndex).matches(Pattern.compile(OR_DSM_ALIAS_REGEX).pattern());
                int orPrecedeIndex = filter.indexOf(Filter.OR_TRIMMED, orIndex + MINIMUM_STEP_FROM_OPERATOR);
                while (orPrecedeIndex != -1 && !filter.substring(orPrecedeIndex, orPrecedeIndex + OR_PATTERN_MATCHER_NUMBER).matches(Pattern.compile(
                        OR_DSM_ALIAS_REGEX).pattern())) {
                    orPrecedeIndex = filter.indexOf(Filter.OR_TRIMMED, orPrecedeIndex + MINIMUM_STEP_FROM_OPERATOR);
                }
                int andPrecedeIndex = filter.indexOf(Filter.AND_TRIMMED, orIndex + MINIMUM_STEP_FROM_OPERATOR);
                while (andPrecedeIndex != -1 && !filter.substring(andPrecedeIndex, andPrecedeIndex + AND_PATTERN_MATCHER_NUMBER).matches(Pattern.compile(
                        AND_DSM_ALIAS_REGEX).pattern())) {
                    andPrecedeIndex = filter.indexOf(Filter.AND_TRIMMED, andPrecedeIndex + MINIMUM_STEP_FROM_OPERATOR);
                }
                if (orPrecedeIndex < andPrecedeIndex && matches) {
                    filterByLogicalOperators.get(Filter.OR_TRIMMED).add(filter.substring(orIndex + MINIMUM_STEP_FROM_OPERATOR, orPrecedeIndex == -1 ? andPrecedeIndex : orPrecedeIndex).trim());
                    orIndex = orPrecedeIndex;
                }
                else if (andPrecedeIndex < orPrecedeIndex && matches) {
                    filterByLogicalOperators.get(Filter.OR_TRIMMED).add(filter.substring(orIndex + MINIMUM_STEP_FROM_OPERATOR, andPrecedeIndex == -1 ? orPrecedeIndex : andPrecedeIndex).trim());
                    orIndex = orPrecedeIndex;
                } else {
                    filterByLogicalOperators.get(Filter.OR_TRIMMED).add(filter.substring(orIndex + MINIMUM_STEP_FROM_OPERATOR).trim());
                    orIndex = orPrecedeIndex;
                }
            }

        }

        return filterByLogicalOperators;
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
        String aliasRegex = "AND".equals(operator) ? AND_DSM_ALIAS_REGEX : OR_DSM_ALIAS_REGEX;
        while (startIndex != -1 && !filter.substring(startIndex, startIndex + patternMatcherNumber).matches(aliasRegex)) {
            startIndex = findNextOperatorIndex(operator, startIndex + nextOperatorFromNumber);
        }
        return startIndex;
    }

    private int findNextOperatorIndex(String operator, int fromIndex) {
        return filter.indexOf(operator, fromIndex);
    }

}
