package org.broadinstitute.dsm.model.elastic.filter;

import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class CollectionQueryBuilderTest {

    @Test
    public void parseFiltersByLogicalOperators() {
        String filter = "AND m.medicalRecordId = 15 OR m.medicalRecordSomething LIKE 55555 OR m.medicalRecordSomethingg = 55552 AND m.dynamicFields.ragac = 55 OR m.medicalRecordName = 213";
        CollectionQueryBuilder collectionQueryBuilder = new CollectionQueryBuilder(filter);
        Map<String, List<String>> parsedFilters = collectionQueryBuilder.parseFiltersByLogicalOperators();
        for (Map.Entry<String, List<String>> eachFilter: parsedFilters.entrySet()) {
            if (eachFilter.getKey().equals("AND")) {
                Assert.assertArrayEquals(new ArrayList<>(List.of("m.medicalRecordId = 15", "m.dynamicFields.ragac = 55")).toArray(), eachFilter.getValue().toArray());
            } else {
                Assert.assertArrayEquals(new ArrayList<>(List.of("m.medicalRecordSomething LIKE 55555", "m.medicalRecordSomethingg = 55552", "m.medicalRecordName = 213")).toArray(), eachFilter.getValue().toArray());
            }
        }
    }


}