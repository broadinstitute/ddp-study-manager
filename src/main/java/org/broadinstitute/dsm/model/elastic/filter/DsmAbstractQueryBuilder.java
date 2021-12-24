package org.broadinstitute.dsm.model.elastic.filter;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.dsm.model.Filter;
import org.broadinstitute.dsm.model.elastic.Util;
import org.broadinstitute.dsm.model.elastic.export.generate.BaseGenerator;
import org.broadinstitute.dsm.model.elastic.export.parse.Parser;
import org.broadinstitute.dsm.statics.DBConstants;
import org.broadinstitute.dsm.statics.ESObjectConstants;
import org.elasticsearch.index.query.AbstractQueryBuilder;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public abstract class DsmAbstractQueryBuilder {

    protected static final String DSM_WITH_DOT = ESObjectConstants.DSM + DBConstants.ALIAS_DELIMITER;
    public static final String DSM_ALIAS_REGEX = "(m|p|r|t|d|oD|o|k|JS|\\()(\\.|\\s)*([a-z]|O)";
    public static final String OR_DSM_ALIAS_REGEX = "(OR) " + DSM_ALIAS_REGEX;
    public static final String AND_DSM_ALIAS_REGEX = "(AND) " + DSM_ALIAS_REGEX;
    public static final int AND_PATTERN_MATCHER_NUMBER = 7;
    public static final int OR_PATTERN_MATCHER_NUMBER = 6;
    public static final int MINIMUM_STEP_FROM_OPERATOR = 3;
    protected String filter;
    protected Parser parser;
    protected BoolQueryBuilder boolQueryBuilder;
    protected QueryBuilder queryBuilder;
    protected BaseSplitter splitter;

    public DsmAbstractQueryBuilder(String filter, Parser parser) {
        this();
        this.parser = parser;
        this.filter = filter;
    }

    public static DsmAbstractQueryBuilder of(String alias) {
        DsmAbstractQueryBuilder queryBuilder;
        boolean isCollection = Util.TABLE_ALIAS_MAPPINGS.get(alias).isCollection();
        return isCollection ? new CollectionQueryBuilder() : new SingleQueryBuilder();
    }

    public DsmAbstractQueryBuilder() {
        boolQueryBuilder = new BoolQueryBuilder();
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

    public void setParser(Parser parser) {
        this.parser = parser;
    }

    public AbstractQueryBuilder build() {
        Map<String, List<String>> parsedFilters = parseFiltersByLogicalOperators();
        for (Map.Entry<String, List<String>> parsedFilter: parsedFilters.entrySet()) {
            List<String> filterValues = parsedFilter.getValue();
            if (parsedFilter.getKey().equals(Filter.AND_TRIMMED)) {
                buildUpQuery(filterValues, BoolQueryBuilder::must);
            } else {
                buildUpQuery(filterValues, BoolQueryBuilder::should);
            }
        }
        return boolQueryBuilder;
    }

    protected void buildUpQuery(List<String> filterValues, FilterStrategy filterStrategy) {
        for (String filterValue : filterValues) {
            Operator operator = Operator.extract(filterValue);
            splitter = SplitterFactory.createSplitter(operator, filterValue);
            splitter.setFilter(filterValue);
            QueryPayload queryPayload = new QueryPayload(buildPath(), splitter.getInnerProperty(), parser.parse(splitter.getValue()));
            queryBuilder = QueryBuilderFactory.buildQueryBuilder(operator, queryPayload);
            buildEachQuery(filterStrategy);
        }
    }

    protected String buildPath() {
        return DSM_WITH_DOT + Util.TABLE_ALIAS_MAPPINGS.get(splitter.getAlias()).getPropertyName();
    }

    protected abstract void buildEachQuery(FilterStrategy filterStrategy);

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

    private String cleanUpData(String filterValue) {
        final Pattern additionalValuesJsonToClean = Pattern.compile("([a-zA-Z]+\\.(additional_values_json IS NOT NULL))");
        return filterValue.replaceAll(additionalValuesJsonToClean.pattern(), StringUtils.EMPTY);
    }
}
