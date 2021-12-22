package org.broadinstitute.dsm.model.elastic.filter;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.dsm.model.Filter;
import org.broadinstitute.dsm.model.elastic.Util;
import org.broadinstitute.dsm.model.elastic.export.parse.Parser;
import org.broadinstitute.dsm.statics.DBConstants;
import org.broadinstitute.dsm.statics.ESObjectConstants;
import org.elasticsearch.index.query.AbstractQueryBuilder;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;

import java.util.*;

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
        Map<String, List<String>> filterByLogicalOperators = new HashMap<>(Map.of(Filter.AND_TRIMMED, new ArrayList<>(), Filter.OR_TRIMMED,
                new ArrayList<>()));
        String[] andSeparated = filter.split(Filter.AND_TRIMMED);
        for (String eachFilter : andSeparated) {
            String cleanedEachFilter = eachFilter.trim();
            if (cleanedEachFilter.contains(Filter.OR_TRIMMED) && !cleanedEachFilter.startsWith(Filter.OPEN_PARENTHESIS) && !cleanedEachFilter.endsWith(Filter.CLOSE_PARENTHESIS)) {
                String[] orSeparated = cleanedEachFilter.split(Filter.OR_TRIMMED);
                filterByLogicalOperators.get(Filter.AND_TRIMMED).add(orSeparated[0].trim());
                Arrays.stream(orSeparated)
                        .skip(1)
                        .forEach(f -> filterByLogicalOperators.get(Filter.OR_TRIMMED).add(f.trim()));
            } else if (StringUtils.isNotBlank(cleanedEachFilter)){
                filterByLogicalOperators.get(Filter.AND_TRIMMED).add(cleanedEachFilter);
            }
        }
        return filterByLogicalOperators;
    }
}
