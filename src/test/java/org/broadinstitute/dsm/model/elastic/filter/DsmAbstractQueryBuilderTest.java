package org.broadinstitute.dsm.model.elastic.filter;

import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.index.query.AbstractQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.NestedQueryBuilder;
import org.junit.Assert;
import org.junit.Test;

public class DsmAbstractQueryBuilderTest {

    @Test
    public void collectionBuild() {

        String filter = "AND m.medicalRecordId = 15";

        DsmAbstractQueryBuilder dsmAbstractQueryBuilder = new CollectionQueryBuilder(filter);

        AbstractQueryBuilder query = dsmAbstractQueryBuilder.build();

        String filterJson = query.toString();
        String nestedKey = "\"dsm.medicalRecord.medicalRecordId\" : ";
        String queryValue = "\"query\" : \"15\"";
        Assert.assertTrue(filterJson.contains(nestedKey));
        Assert.assertTrue(filterJson.contains(queryValue));
    }

    @Test
    public void singleBuild() {

        String filter = "AND m.medicalRecordId = 15";

        DsmAbstractQueryBuilder dsmAbstractQueryBuilder = new DsmAbstractQueryBuilder(filter) {

            @Override
            public AbstractQueryBuilder build() {

                return null;
            }
        };

        String expected = "";

        AbstractQueryBuilder query = dsmAbstractQueryBuilder.build();

        Assert.assertEquals(expected, query);

    }


}
