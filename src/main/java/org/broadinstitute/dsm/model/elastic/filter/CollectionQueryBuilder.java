package org.broadinstitute.dsm.model.elastic.filter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.elasticsearch.index.query.AbstractQueryBuilder;

public class CollectionQueryBuilder extends DsmAbstractQueryBuilder {

    public CollectionQueryBuilder(String filter) {
        super(filter);
    }

    @Override
    public AbstractQueryBuilder build() {
        Map<String, List<String>> filterByLogicalOperators = new HashMap<>();
        String filter = "AND m.medicalRecordId = 15 AND m.dynamicFields.ragac = 55 OR m.medicalRecordName = 213";
        String[] andSeparated = filter.split("AND");
        for (String eachFilter : andSeparated) {
            String cleanedEachFilter = eachFilter.trim();
            if (cleanedEachFilter.contains("OR")) {

            } else {
                filterByLogicalOperators.compute("AND", (curr, prev) -> {
                    if (Objects.isNull(prev)) {
                        List<String> filters = new ArrayList<>();
                        filters.add(cleanedEachFilter);
                        return filters;
                    } else {
                        prev.add(cleanedEachFilter);
                        return prev;
                    }
                });

            }
        }

        return null;
    }
}
