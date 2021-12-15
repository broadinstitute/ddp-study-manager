package org.broadinstitute.dsm.model.elastic.filter;

import java.util.*;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.search.join.ScoreMode;
import org.broadinstitute.dsm.model.elastic.Util;
import org.broadinstitute.dsm.statics.ESObjectConstants;
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
            if (parsedFilter.getKey().equals("AND")) {
                List<String> filterValues = parsedFilter.getValue();
                for (String filterValue : filterValues) {
                    String[] splittedFilter = filterValue.split("=");
                    String value = splittedFilter[1].trim();
                    String[] aliasWithField = splittedFilter[0].trim().split(ElasticSearchUtil.DOT_SEPARATOR);
                    String innerProperty = aliasWithField[1];// medicalRecordId
                    String alias = aliasWithField[0];
                    String outerProperty = Util.TABLE_ALIAS_MAPPINGS.get(alias).getPropertyName(); //medicalRecord
                    String nestedPath = DSM_WITH_DOT + outerProperty;
                    boolQueryBuilder.must(new NestedQueryBuilder(nestedPath, new MatchQueryBuilder(nestedPath + "." + innerProperty, value), ScoreMode.Avg));
                }
            } else {
                boolQueryBuilder.should();
            }
        }
        return null;
    }

    protected Map<String, List<String>> parseFiltersByLogicalOperators() {
        Map<String, List<String>> filterByLogicalOperators = new HashMap<>(Map.of("AND", new ArrayList<>(), "OR", new ArrayList<>()));
        String[] andSeparated = filter.split("AND");
        for (String eachFilter : andSeparated) {
            String cleanedEachFilter = eachFilter.trim();
            if (cleanedEachFilter.contains("OR")) {
                String[] orSeparated = cleanedEachFilter.split("OR");
                filterByLogicalOperators.get("AND").add(orSeparated[0].trim());
                Arrays.stream(orSeparated)
                        .skip(1)
                        .forEach(f -> filterByLogicalOperators.get("OR").add(f.trim()));
            } else if (StringUtils.isNotBlank(cleanedEachFilter)){
                filterByLogicalOperators.get("AND").add(cleanedEachFilter);
            }
        }
        return filterByLogicalOperators;
    }
}
