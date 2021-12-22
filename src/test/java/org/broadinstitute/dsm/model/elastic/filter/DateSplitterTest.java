package org.broadinstitute.dsm.model.elastic.filter;

import org.junit.Assert;
import org.junit.Test;

public class DateSplitterTest {

    @Test
    public void split() {

        String filter = "STR_TO_DATE(m.fax_sent,'%Y-%m-%d') = STR_TO_DATE('2021-12-17','%Y-%m-%d')";
        BaseSplitter dateSplitter = SplitterFactory.createSplitter(Operator.DATE, filter);

        Assert.assertEquals("m", dateSplitter.getAlias());
        Assert.assertEquals("faxSent", dateSplitter.getInnerProperty());
        Assert.assertEquals("'2021-12-17'", dateSplitter.getValue()[0]);
    }
}