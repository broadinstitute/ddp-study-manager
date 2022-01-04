package org.broadinstitute.dsm.model.elastic.export.parse;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import org.broadinstitute.dsm.model.elastic.export.generate.MappingGenerator;

public class TypeParser extends BaseParser {

    private static final String TEXT = "text";
    private static final String FIELDS = "fields";
    private static final String KEYWORD = "keyword";
    protected static final String TYPE = "type";
    public static final Map<String, Object> TEXT_KEYWORD_MAPPING = new HashMap<>(
            new HashMap<>(
                    Map.of(TYPE, TEXT,
                    FIELDS, new HashMap<>(Map.of(KEYWORD, new HashMap<>(Map.of(TYPE, KEYWORD))
            )))));
    private static final String BOOLEAN = "boolean";
    public static final Map<String, String> BOOLEAN_MAPPING = new HashMap<>(Map.of(MappingGenerator.TYPE, BOOLEAN));
    public static final String DATE = "date";
    public static final Map<String, String> DATE_MAPPING = new HashMap<>(Map.of(MappingGenerator.TYPE, DATE));
    public static final String LONG = "long";
    public static final Map<String, String> LONG_MAPPING = new HashMap<>(Map.of(MappingGenerator.TYPE, LONG));

    @Override
    public Object parse(String fieldName) {
        Class<?> propertyClass = propertyInfo.getPropertyClass();
        Object mappingType;
        try {
            Field field = propertyClass.getField(fieldName);
            if (long.class.isAssignableFrom(field.getType())) {
                mappingType = LONG_MAPPING;
            } else if (boolean.class.isAssignableFrom(field.getType())) {
                mappingType = BOOLEAN_MAPPING;
            } else {
                // either text or date in string
            }
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
        return super.parse(fieldName);
    }

    @Override
    protected Object forNumeric(String value) {
        return LONG_MAPPING;
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