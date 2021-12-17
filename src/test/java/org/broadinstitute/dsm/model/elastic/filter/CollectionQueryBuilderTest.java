package org.broadinstitute.dsm.model.elastic.filter;

import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.index.query.*;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class CollectionQueryBuilderTest {

    @Test
    public void parseFiltersByLogicalOperators() {
        String filter = "AND m.medicalRecordId = '15' OR m.medicalRecordSomething LIKE '55555' OR m.medicalRecordSomethingg = '55552' AND m.dynamicFields.ragac = '55' OR m.medicalRecordName = '213'";
        CollectionQueryBuilder collectionQueryBuilder = new CollectionQueryBuilder(filter);
        Map<String, List<String>> parsedFilters = collectionQueryBuilder.parseFiltersByLogicalOperators();
        for (Map.Entry<String, List<String>> eachFilter: parsedFilters.entrySet()) {
            if (eachFilter.getKey().equals("AND")) {
                Assert.assertArrayEquals(new ArrayList<>(List.of("m.medicalRecordId = '15'", "m.dynamicFields.ragac = '55'")).toArray(),
                        eachFilter.getValue().toArray());
            } else {
                Assert.assertArrayEquals(new ArrayList<>(List.of("m.medicalRecordSomething LIKE '55555'", "m.medicalRecordSomethingg = '55552'", "m.medicalRecordName = '213'")).toArray(), eachFilter.getValue().toArray());
            }
        }
    }

    @Test
    public void collectionBuild() {

        String filter = "AND m.medicalRecordId = '15' AND m.type = 'PHYSICIAN' OR k.bspCollaboratorSampleId = 'ASCProject_PZ8GJC_SALIVA'";

        DsmAbstractQueryBuilder dsmAbstractQueryBuilder = new CollectionQueryBuilder(filter);

        AbstractQueryBuilder actual = dsmAbstractQueryBuilder.build();

        AbstractQueryBuilder<BoolQueryBuilder> expected = new BoolQueryBuilder().must(new NestedQueryBuilder("dsm.medicalRecord", new MatchQueryBuilder("dsm.medicalRecord.medicalRecordId", "15"), ScoreMode.Avg))
                .must(new NestedQueryBuilder("dsm.medicalRecord", new MatchQueryBuilder("dsm.medicalRecord.type", "PHYSICIAN"), ScoreMode.Avg))
                .should(new NestedQueryBuilder("dsm.kitRequestShipping", new MatchQueryBuilder("dsm.kitRequestShipping.bspCollaboratorSampleId", "ASCProject_PZ8GJC_SALIVA"), ScoreMode.Avg));

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void collectionBuild2() {

        String filter = "AND m.medicalRecordId >= '15' AND m.type LIKE 'PHYSICIAN' OR k.bspCollaboratorSampleId = 'ASCProject_PZ8GJC_SALIVA' AND t.returnDate <= '2015-01-01' AND p.participantId IS NOT NULL";

        DsmAbstractQueryBuilder dsmAbstractQueryBuilder = new CollectionQueryBuilder(filter);

        AbstractQueryBuilder actual = dsmAbstractQueryBuilder.build();

        AbstractQueryBuilder<BoolQueryBuilder> expected = new BoolQueryBuilder().must(new NestedQueryBuilder("dsm.medicalRecord", new RangeQueryBuilder("dsm.medicalRecord.medicalRecordId").gte("15"), ScoreMode.Avg))
                .must(new NestedQueryBuilder("dsm.medicalRecord", new MatchQueryBuilder("dsm.medicalRecord.type", "PHYSICIAN"), ScoreMode.Avg))
                .should(new NestedQueryBuilder("dsm.kitRequestShipping", new MatchQueryBuilder("dsm.kitRequestShipping.bspCollaboratorSampleId", "ASCProject_PZ8GJC_SALIVA"), ScoreMode.Avg))
                .must(new NestedQueryBuilder("dsm.tissue", new RangeQueryBuilder("dsm.tissue.returnDate").lte("2015-01-01"), ScoreMode.Avg))
                .must(new NestedQueryBuilder("dsm.participant", new ExistsQueryBuilder("dsm.participant.participantId"), ScoreMode.Avg));

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void collectionBuildAgeRange() {

        String filter = "AND m.age >= '15' AND m.age <= '30'";

        DsmAbstractQueryBuilder dsmAbstractQueryBuilder = new CollectionQueryBuilder(filter);

        AbstractQueryBuilder actual = dsmAbstractQueryBuilder.build();

        AbstractQueryBuilder<BoolQueryBuilder> expected = new BoolQueryBuilder().must(new NestedQueryBuilder("dsm.medicalRecord",
                        new RangeQueryBuilder("dsm.medicalRecord.age").gte("15"), ScoreMode.Avg))
                .must(new NestedQueryBuilder("dsm.medicalRecord", new RangeQueryBuilder("dsm.medicalRecord.age").lte("30"),
                        ScoreMode.Avg));

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void collectionBuildBoolean() {

        String filter = "AND m.followUp LIKE '1'";

        DsmAbstractQueryBuilder dsmAbstractQueryBuilder = new CollectionQueryBuilder(filter);

        AbstractQueryBuilder actual = dsmAbstractQueryBuilder.build();

        AbstractQueryBuilder<BoolQueryBuilder> expected = new BoolQueryBuilder().must(new NestedQueryBuilder("dsm.medicalRecord",
                        new MatchQueryBuilder("dsm.medicalRecord.followUp", "'1'"), ScoreMode.Avg));

        Assert.assertEquals(expected, actual);
    }



}