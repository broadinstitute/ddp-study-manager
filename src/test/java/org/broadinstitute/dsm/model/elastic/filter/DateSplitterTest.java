package org.broadinstitute.dsm.model.elastic.filter;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.*;

public class DateSplitterTest {

    @Test
    public void split() {

        String filter = "DATE(FROM_UNIXTIME(k.scan_date/1000))  = DATE(FROM_UNIXTIME(1640563200))";
        BaseSplitter splitter = SplitterFactory.createSplitter(Operator.DATE, filter);
        splitter.setFilter(filter);

        Assert.assertEquals("1640563200", splitter.getValue()[0]);
        Assert.assertEquals("scanDate", splitter.getInnerProperty());

    }
}