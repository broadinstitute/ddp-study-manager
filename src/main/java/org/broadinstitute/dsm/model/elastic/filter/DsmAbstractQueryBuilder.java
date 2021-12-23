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
import java.util.regex.Pattern;

public abstract class DsmAbstractQueryBuilder {

    protected static final String DSM_WITH_DOT = ESObjectConstants.DSM + DBConstants.ALIAS_DELIMITER;
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
        Map<String, List<String>> filterByLogicalOperators = new HashMap<>(Map.of(Filter.AND_TRIMMED, new ArrayList<>(), Filter.OR_TRIMMED, new ArrayList<>()));
        // (OR|AND) (m|p|r|t|d|oD|o)\.
        int flag = 0;
        int andIndex = filter.indexOf(Filter.AND_TRIMMED);
        int orIndex = filter.indexOf(Filter.OR_TRIMMED);
        while (andIndex != -1 || orIndex != -1) {
            if (andIndex != -1) {

                // substring -> exclusive
                int filterIndex = andIndex + 7;
                boolean matches = filter.substring(andIndex, filterIndex).matches(Pattern.compile("(AND) (m|p|r|t|d|oD|o)\\.[a-z]").pattern());
                // AND m.red = '5' OR k.kkk = '5'

                int orPrecedeIndex = filter.indexOf("OR", andIndex + 3); // pirveli OR
                while (orPrecedeIndex != -1 && !filter.substring(orPrecedeIndex, orPrecedeIndex + 6).matches(Pattern.compile("(OR) (m|p|r|t|d|oD|o)\\.[a-z]").pattern())) {
                    orPrecedeIndex = filter.indexOf("OR", orPrecedeIndex + 3);
                }

                int andPrecedeIndex = filter.indexOf("AND", andIndex + 3); // shemdegi AND
                while (andPrecedeIndex != -1 && !filter.substring(andPrecedeIndex, andPrecedeIndex + 7).matches(Pattern.compile("(AND) (m|p|r|t|d|oD|o)\\.[a-z]").pattern())) {
                    andPrecedeIndex = filter.indexOf("AND", andPrecedeIndex + 3);
                }

                if (orPrecedeIndex < andPrecedeIndex && matches) {
                    filterByLogicalOperators.get("AND").add(filter.substring(andIndex + 3, orPrecedeIndex == -1 ? andPrecedeIndex : orPrecedeIndex).trim());
//                    orIndex = orPrecedeIndex;
                    andIndex = andPrecedeIndex;
                }
                else if (andPrecedeIndex < orPrecedeIndex && matches) {
                    filterByLogicalOperators.get("AND").add(filter.substring(andIndex + 3, andPrecedeIndex == -1 ? orPrecedeIndex : andPrecedeIndex).trim());
                    andIndex = andPrecedeIndex;
//                    orIndex = orPrecedeIndex;
                } else {
                    filterByLogicalOperators.get("AND").add(filter.substring(andIndex + 3).trim());
                    andIndex = andPrecedeIndex;
                }
            } else {
                int filterIndex = orIndex + 6;
                boolean matches = filter.substring(orIndex, filterIndex).matches(Pattern.compile("(OR) (m|p|r|t|d|oD|o)\\.[a-z]").pattern());
                int orPrecedeIndex = filter.indexOf("OR", orIndex + 3);
                while (orPrecedeIndex != -1 && !filter.substring(orPrecedeIndex, orPrecedeIndex + 6).matches(Pattern.compile("(OR) (m|p|r|t|d|oD|o)\\.[a-z]").pattern())) {
                    orPrecedeIndex = filter.indexOf("OR", orPrecedeIndex + 3);
                }
                int andPrecedeIndex = filter.indexOf("AND", orIndex + 3);
                while (andPrecedeIndex != -1 && !filter.substring(andPrecedeIndex, andPrecedeIndex + 7).matches(Pattern.compile("(AND) (m|p|r|t|d|oD|o)\\.[a-z]").pattern())) {
                    andPrecedeIndex = filter.indexOf("AND", andPrecedeIndex + 3);
                }
                if (orPrecedeIndex < andPrecedeIndex && matches) {
                    filterByLogicalOperators.get("OR").add(filter.substring(orIndex + 3, orPrecedeIndex == -1 ? andPrecedeIndex : orPrecedeIndex).trim());
                    orIndex = orPrecedeIndex;
//                    andIndex = andPrecedeIndex;
                }
                else if (andPrecedeIndex < orPrecedeIndex && matches) {
                    filterByLogicalOperators.get("OR").add(filter.substring(orIndex + 3, andPrecedeIndex == -1 ? orPrecedeIndex : andPrecedeIndex).trim());
                    orIndex = orPrecedeIndex;
//                    andIndex = andPrecedeIndex;
                } else {
                    filterByLogicalOperators.get("OR").add(filter.substring(orIndex + 3).trim());
                    orIndex = orPrecedeIndex;
                }
            }

        }

//
//        String[] andSeparated = filter.split(Filter.AND_TRIMMED);
//
//        for (String eachFilter : andSeparated) {
//            String cleanedEachFilter = cleanUpData(eachFilter).trim();
//            if (cleanedEachFilter.startsWith(Filter.OR_TRIMMED) && !cleanedEachFilter.startsWith(Filter.OPEN_PARENTHESIS) && !cleanedEachFilter.endsWith(Filter.CLOSE_PARENTHESIS)) {
//                String[] orSeparated = cleanedEachFilter.split(Filter.OR_TRIMMED);
//                filterByLogicalOperators.get(Filter.AND_TRIMMED).add(orSeparated[0].trim());
//                Arrays.stream(orSeparated)
//                        .skip(1)
//                        .forEach(f -> filterByLogicalOperators.get(Filter.OR_TRIMMED).add(f.trim()));
//            } else if (StringUtils.isNotBlank(cleanedEachFilter)){
//                filterByLogicalOperators.get(Filter.AND_TRIMMED).add(cleanedEachFilter);
//            }
//        }
        return filterByLogicalOperators;
    }

    private String cleanUpData(String filterValue) {
        final Pattern additionalValuesJsonToClean = Pattern.compile("([a-zA-Z]+\\.(additional_values_json IS NOT NULL))");
        return filterValue.replaceAll(additionalValuesJsonToClean.pattern(), StringUtils.EMPTY);
    }
}
