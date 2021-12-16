package org.broadinstitute.dsm.model.elastic.filter;

import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.index.query.AbstractQueryBuilder;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.NestedQueryBuilder;
import org.junit.Assert;
import org.junit.Test;

public class DsmAbstractQueryBuilderTest {

    @Test
    public void collectionBuild() {

        String filter = "AND m.medicalRecordId = 15 AND m.type = PHYSICIAN OR k.bspCollaboratorSampleId = ASCProject_PZ8GJC_SALIVA";

        DsmAbstractQueryBuilder dsmAbstractQueryBuilder = new CollectionQueryBuilder(filter);

        AbstractQueryBuilder actual = dsmAbstractQueryBuilder.build();

        AbstractQueryBuilder<BoolQueryBuilder> expected = new BoolQueryBuilder().must(new NestedQueryBuilder("dsm.medicalRecord", new MatchQueryBuilder("dsm.medicalRecord.medicalRecordId", "15"), ScoreMode.Avg))
                .must(new NestedQueryBuilder("dsm.medicalRecord", new MatchQueryBuilder("dsm.medicalRecord.type", "PHYSICIAN"), ScoreMode.Avg))
                .should(new NestedQueryBuilder("dsm.kitRequestShipping", new MatchQueryBuilder("dsm.kitRequestShipping.bspCollaboratorSampleId", "ASCProject_PZ8GJC_SALIVA"), ScoreMode.Avg));

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void singleBuild() {


    }

}
