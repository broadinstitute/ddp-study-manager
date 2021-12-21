package org.broadinstitute.dsm.model.elastic.filter;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

public class JsonExtractSplitterTest {

    static String filter;
    static BaseSplitter splitter;

    @BeforeClass
    public static void setUp() {
        filter = "JSON_EXTRACT ( m.additional_values_json , '$.seeingIfBugExists' ) = 'true'";
        splitter = SplitterFactory.createSplitter(Operator.JSON_EXTRACT);
        splitter.setFilter(filter);
    }

    @Test
    public void getValue() {
        Assert.assertEquals("'true'", splitter.getValue()[0]);
    }

    @Test
    public void getInnerProperty() {
        Assert.assertEquals("dynamicFields", splitter.getInnerProperty());
    }
}