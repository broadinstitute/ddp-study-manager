package org.broadinstitute.dsm.model.elastic.filter;

import org.broadinstitute.dsm.model.elastic.export.parse.BaseParser;
import org.junit.Assert;
import org.junit.Test;

public class FilterParserTest {

    @Test
    public void parse() {

        String trueValue = "'1'";
        String falseValue = "NOT'1'";
        String str = "'string'";
        String number = "'5'";
        String date = "'1999-05-22'";

        BaseParser filterParser = new FilterParser();
        Assert.assertEquals(true, filterParser.parse(trueValue));
        Assert.assertEquals(false, filterParser.parse(falseValue));
        Assert.assertEquals("string", filterParser.parse(str));
        Assert.assertEquals("5", filterParser.parse(number));
        Assert.assertEquals("1999-05-22", filterParser.parse(date));
    }

}