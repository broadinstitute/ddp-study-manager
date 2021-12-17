package org.broadinstitute.dsm.model.elastic.export.parse;

import static org.junit.Assert.*;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class BaseParserTest {

    static BaseParser valueParser;

    @BeforeClass
    public static void setUp() {
        valueParser = new ValueParser();
    }

    @Test
    public void isBoolean() {
        String falseValue = "'0'";
        String trueValue = "'1'";
        assertTrue(valueParser.isBoolean(falseValue));
        assertTrue(valueParser.isBoolean(trueValue));
    }

    @Test
    public void parse() {
        String value = "'15'";
        String convertedValue = valueParser.convertString(value);
        Assert.assertEquals("15", convertedValue);
    }
}