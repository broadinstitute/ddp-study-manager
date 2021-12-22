package org.broadinstitute.dsm.model.elastic.filter;

import org.elasticsearch.index.query.AbstractQueryBuilder;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class SingleQueryBuilderTest {

    SingleQueryBuilder singleQueryBuilder;

    @Before
    public void setUp() {
        singleQueryBuilder = new SingleQueryBuilder();
        singleQueryBuilder.setParser(new FilterParser());
    }

    @Test
    public void buildEachQuery() {

        String filter = "AND p.participantId = '1234'";
        singleQueryBuilder.setFilter(filter);

        AbstractQueryBuilder actual = singleQueryBuilder.build();
        BoolQueryBuilder expected = new BoolQueryBuilder().must(new MatchQueryBuilder("dsm.participant.participantId", "1234"));

        Assert.assertEquals(expected, actual);
    }

}