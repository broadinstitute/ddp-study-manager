package org.broadinstitute.dsm.model.elastic.filter;

import java.util.*;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.search.join.ScoreMode;
import org.broadinstitute.dsm.model.Filter;
import org.broadinstitute.dsm.model.elastic.Util;
import org.broadinstitute.dsm.util.ElasticSearchUtil;
import org.elasticsearch.index.query.*;

public class CollectionQueryBuilder extends DsmAbstractQueryBuilder {

    public CollectionQueryBuilder(String filter) {
        super(filter);
    }

    @Override
    public AbstractQueryBuilder build() {
        Map<String, List<String>> parsedFilters = parseFiltersByLogicalOperators();
        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
        for (Map.Entry<String, List<String>> parsedFilter: parsedFilters.entrySet()) {
            List<String> filterValues = parsedFilter.getValue();
            if (parsedFilter.getKey().equals(Filter.AND)) {
                buildUpNestedQuery(boolQueryBuilder, filterValues, BoolQueryBuilder::must);
            } else {
                buildUpNestedQuery(boolQueryBuilder, filterValues, BoolQueryBuilder::should);
            }
        }
        return boolQueryBuilder;
    }

    private void buildUpNestedQuery(BoolQueryBuilder boolQueryBuilder, List<String> filterValues, FilterStrategy filterStrategy) {
        for (String filterValue : filterValues) {
            BaseSplitter splitter = SplitterFactory.createSplitter(filterValue);
            String outerProperty = Util.TABLE_ALIAS_MAPPINGS.get(splitter.getAlias()).getPropertyName(); //medicalRecord
            String nestedPath = DSM_WITH_DOT + outerProperty;
            filterStrategy.build(boolQueryBuilder, buildNestedQueryBuilder(nestedPath, splitter.getInnerProperty(), splitter.getValue()));
        }
    }

    private NestedQueryBuilder buildNestedQueryBuilder(String path, String fieldName, Object value) {
        return new NestedQueryBuilder(path, new MatchQueryBuilder(path + "." + fieldName, value), ScoreMode.Avg);
    }

    protected Map<String, List<String>> parseFiltersByLogicalOperators() {
        Map<String, List<String>> filterByLogicalOperators = new HashMap<>(Map.of(Filter.AND, new ArrayList<>(), Filter.OR, new ArrayList<>()));
        String[] andSeparated = filter.split(Filter.AND_TRIMMED);
        for (String eachFilter : andSeparated) {
            String cleanedEachFilter = eachFilter.trim();
            if (cleanedEachFilter.contains(Filter.OR)) {
                String[] orSeparated = cleanedEachFilter.split(Filter.OR);
                filterByLogicalOperators.get(Filter.AND).add(orSeparated[0].trim());
                Arrays.stream(orSeparated)
                        .skip(1)
                        .forEach(f -> filterByLogicalOperators.get(Filter.OR).add(f.trim()));
            } else if (StringUtils.isNotBlank(cleanedEachFilter)){
                filterByLogicalOperators.get(Filter.AND).add(cleanedEachFilter);
            }
        }
        return filterByLogicalOperators;
    }
}

