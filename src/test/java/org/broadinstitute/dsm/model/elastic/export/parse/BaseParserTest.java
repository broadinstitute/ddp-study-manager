package org.broadinstitute.dsm.model.elastic.export.parse;

import static org.junit.Assert.*;

import org.junit.Test;

public class BaseParserTest {

    @Test
    public void isBoolean() {
        String falseValue = "'0'";
        String trueValue = "'1'";
        BaseParser valueParser = new ValueParser();
        assertTrue(valueParser.isBoolean(falseValue));
        assertTrue(valueParser.isBoolean(trueValue));
    }

    @Test
    public void convertBoolean() {
        String falseValue = "'0'";
        String trueValue = "'1'";
        BaseParser valueParser = new ValueParser();
        boolean falseBool = valueParser.convertBoolean(falseValue);
        boolean trueBool = valueParser.convertBoolean(falseValue);
        assertTrue(trueBool);
        assertFalse(falseBool);
    }
}