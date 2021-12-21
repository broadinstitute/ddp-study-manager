package org.broadinstitute.dsm.model.elastic.filter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.index.query.AbstractQueryBuilder;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.ExistsQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.NestedQueryBuilder;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class CollectionQueryBuilderTest {

    static CollectionQueryBuilder collectionQueryBuilder;

    @BeforeClass
    public static void setUp() {
        collectionQueryBuilder = new CollectionQueryBuilder();
        collectionQueryBuilder.setParser(new FilterParser());
    }

    @Test
    public void parseFiltersByLogicalOperators() {
        String filter = "AND m.medicalRecordId = '15' OR m.medicalRecordSomething LIKE '55555' OR m.medicalRecordSomethingg = '55552' AND m.dynamicFields.ragac = '55' OR m.medicalRecordName = '213'";
        collectionQueryBuilder.setFilter(filter);
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

        collectionQueryBuilder.setFilter(filter);
        AbstractQueryBuilder actual = collectionQueryBuilder.build();

        AbstractQueryBuilder<BoolQueryBuilder> expected = new BoolQueryBuilder().must(new NestedQueryBuilder("dsm.medicalRecord", new MatchQueryBuilder("dsm.medicalRecord.medicalRecordId", "15"), ScoreMode.Avg))
                .must(new NestedQueryBuilder("dsm.medicalRecord", new MatchQueryBuilder("dsm.medicalRecord.type", "PHYSICIAN"), ScoreMode.Avg))
                .should(new NestedQueryBuilder("dsm.kitRequestShipping", new MatchQueryBuilder("dsm.kitRequestShipping.bspCollaboratorSampleId", "ASCProject_PZ8GJC_SALIVA"), ScoreMode.Avg));

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void collectionBuild2() {

        String filter = "AND m.medicalRecordId >= '15' AND m.type LIKE 'PHYSICIAN' OR k.bspCollaboratorSampleId = 'ASCProject_PZ8GJC_SALIVA' AND t.returnDate <= '2015-01-01' AND p.participantId IS NOT NULL";

        collectionQueryBuilder.setFilter(filter);
        AbstractQueryBuilder actual = collectionQueryBuilder.build();

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

        collectionQueryBuilder.setFilter(filter);
        AbstractQueryBuilder actual = collectionQueryBuilder.build();

        AbstractQueryBuilder<BoolQueryBuilder> expected = new BoolQueryBuilder().must(new NestedQueryBuilder("dsm.medicalRecord",
                        new RangeQueryBuilder("dsm.medicalRecord.age").gte("15"), ScoreMode.Avg))
                .must(new NestedQueryBuilder("dsm.medicalRecord", new RangeQueryBuilder("dsm.medicalRecord.age").lte("30"),
                        ScoreMode.Avg));

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void collectionBuildBoolean() {

        String filter = "AND m.followUp LIKE '1'";

        collectionQueryBuilder.setFilter(filter);
        AbstractQueryBuilder actual = collectionQueryBuilder.build();

        AbstractQueryBuilder<BoolQueryBuilder> expected = new BoolQueryBuilder().must(new NestedQueryBuilder("dsm.medicalRecord",
                        new MatchQueryBuilder("dsm.medicalRecord.followUp", true), ScoreMode.Avg));

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void multipleOptionsQueryBuilder() {

        String filter = "AND ( oD.request = 'review' OR oD.request = 'no' OR oD.request = 'hold' OR oD.request = 'request' OR oD.request = 'unable To Obtain' OR oD.request = 'sent' OR oD.request = 'received' OR oD.request = 'returned' )";

        collectionQueryBuilder.setFilter(filter);
        AbstractQueryBuilder actual = collectionQueryBuilder.build();

        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
        boolQueryBuilder.should(new MatchQueryBuilder("dsm.oncHistoryDetail.request", "review"));
        boolQueryBuilder.should(new MatchQueryBuilder("dsm.oncHistoryDetail.request", "no"));
        boolQueryBuilder.should(new MatchQueryBuilder("dsm.oncHistoryDetail.request", "hold"));
        boolQueryBuilder.should(new MatchQueryBuilder("dsm.oncHistoryDetail.request", "request"));
        boolQueryBuilder.should(new MatchQueryBuilder("dsm.oncHistoryDetail.request", "unable To Obtain"));
        boolQueryBuilder.should(new MatchQueryBuilder("dsm.oncHistoryDetail.request", "sent"));
        boolQueryBuilder.should(new MatchQueryBuilder("dsm.oncHistoryDetail.request", "received"));
        boolQueryBuilder.should(new MatchQueryBuilder("dsm.oncHistoryDetail.request", "returned"));

        AbstractQueryBuilder<BoolQueryBuilder> expected = new BoolQueryBuilder().must(new NestedQueryBuilder("dsm.oncHistoryDetail",
                        boolQueryBuilder, ScoreMode.Avg));

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void dateGreaterBuild() {

        String filter = "AND m.received >= STR_TO_DATE('2012-01-01', %yyyy-%MM-%dd) AND m.received <= STR_TO_DATE('2015-01-01', %yyyy-%MM-%dd)";

        collectionQueryBuilder.setFilter(filter);
        AbstractQueryBuilder actual = collectionQueryBuilder.build();

        AbstractQueryBuilder<BoolQueryBuilder> expected = new BoolQueryBuilder().must(new NestedQueryBuilder("dsm.medicalRecord",
                        new RangeQueryBuilder("dsm.medicalRecord.received").gte("2012-01-01"), ScoreMode.Avg))
                .must(new NestedQueryBuilder("dsm.medicalRecord", new RangeQueryBuilder("dsm.medicalRecord.received").lte("2015-01-01"),
                        ScoreMode.Avg));

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void dynamicFieldsQueryBuild() {

        String filter = "AND JSON_EXTRACT ( m.additional_values_json , '$.seeingIfBugExists' ) = 'true' AND m.received <= STR_TO_DATE('2015-01-01', %yyyy-%MM-%dd)";

    }



}