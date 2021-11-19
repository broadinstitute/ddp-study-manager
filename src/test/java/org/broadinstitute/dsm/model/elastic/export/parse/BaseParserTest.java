package org.broadinstitute.dsm.model.elastic.export.parse;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Map;

import org.junit.Test;

public class BaseParserTest {

    @Test
    public void isCollection() {
        String json = "[{\"field1\": \"value1\"}, {\"field2\": \"value2\"}]";
        TypeParser typeParser = new TypeParser();
        boolean isCollection = typeParser.isCollection(json);
        assertTrue(isCollection);
    }

    @Test
    public void isNotCollection() {
        String json = "{\"field1\": \"value1\", \"field2\": \"value2\"}";
        TypeParser typeParser = new TypeParser();
        boolean isCollection = typeParser.isCollection(json);
        assertFalse(isCollection);
    }
}