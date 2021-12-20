package org.broadinstitute.dsm.model.elastic.filter;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.search.join.ScoreMode;
import org.broadinstitute.dsm.model.Filter;
import org.broadinstitute.dsm.model.elastic.Util;
import org.broadinstitute.dsm.model.elastic.export.parse.Parser;
import org.elasticsearch.index.query.*;

import java.util.*;

public class CollectionQueryBuilder extends DsmAbstractQueryBuilder {

    public CollectionQueryBuilder(String filter, Parser parser) {
        super(filter, parser);
    }

    public CollectionQueryBuilder() {}

    @Override
    public AbstractQueryBuilder build() {
        Map<String, List<String>> parsedFilters = parseFiltersByLogicalOperators();
        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
        for (Map.Entry<String, List<String>> parsedFilter: parsedFilters.entrySet()) {
            List<String> filterValues = parsedFilter.getValue();
            if (parsedFilter.getKey().equals(Filter.AND_TRIMMED)) {
                buildUpNestedQuery(boolQueryBuilder, filterValues, BoolQueryBuilder::must);
            } else {
                buildUpNestedQuery(boolQueryBuilder, filterValues, BoolQueryBuilder::should);
            }
        }
        return boolQueryBuilder;
    }

    private void buildUpNestedQuery(BoolQueryBuilder boolQueryBuilder, List<String> filterValues, FilterStrategy filterStrategy) {
        for (String filterValue : filterValues) {
            Operator operator = Operator.extract(filterValue);
            BaseSplitter splitter = SplitterFactory.createSplitter(operator);
            splitter.setFilter(filterValue);
            String outerProperty = Util.TABLE_ALIAS_MAPPINGS.get(splitter.getAlias()).getPropertyName(); //medicalRecord
            String nestedPath = DSM_WITH_DOT + outerProperty;
            QueryPayload queryPayload = new QueryPayload(nestedPath, splitter.getInnerProperty(), parser.parse(splitter.getValue()));
            QueryBuilder queryBuilder = QueryBuilderFactory.buildQueryBuilder(operator, queryPayload);
            filterStrategy.build(boolQueryBuilder, new NestedQueryBuilder(nestedPath, queryBuilder, ScoreMode.Avg));
        }
    }

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

