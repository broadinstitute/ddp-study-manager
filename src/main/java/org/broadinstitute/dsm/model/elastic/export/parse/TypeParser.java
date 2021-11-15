package org.broadinstitute.dsm.model.elastic.export.parse;

import org.broadinstitute.dsm.model.elastic.export.generate.MappingGenerator;

import java.util.Map;

public class TypeParser extends BaseParser {


    private static final String TEXT = "text";
    private static final String FIELDS = "fields";
    private static final String KEYWORD = "keyword";
    private static final String TYPE = "type";
    public static final Map<String, Object> TEXT_KEYWORD_MAPPING = Map.of(TYPE, TEXT, FIELDS, Map.of(KEYWORD, Map.of(TYPE, KEYWORD)));
    private static final String BOOLEAN = "boolean";
    public static final Map<String, String> BOOLEAN_MAPPING = Map.of(MappingGenerator.TYPE, BOOLEAN);
    private static final String DATE = "date";
    public static final Map<String, String> DATE_MAPPING = Map.of(MappingGenerator.TYPE, DATE);

    @Override
    protected Object forNumeric(String value) {
        return TEXT_KEYWORD_MAPPING;
    }

    @Override
    protected Object forBoolean(String value) {
        return BOOLEAN_MAPPING;
    }

    @Override
    protected Object forDate(String value) {
        return DATE_MAPPING;
    }

    @Override
    protected Object forString(String value) {
        return TEXT_KEYWORD_MAPPING;
    }
}