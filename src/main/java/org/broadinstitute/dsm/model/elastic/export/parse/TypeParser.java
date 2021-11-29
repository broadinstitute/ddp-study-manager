package org.broadinstitute.dsm.model.elastic.export.parse;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import org.broadinstitute.dsm.model.elastic.export.generate.MappingGenerator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TypeParser extends BaseParser {


    private static final String TEXT = "text";
    private static final String FIELDS = "fields";
    private static final String KEYWORD = "keyword";
    private static final String TYPE = "type";
    public static final Map<String, Object> TEXT_KEYWORD_MAPPING = new HashMap<>(
            new HashMap<>(
                    Map.of(TYPE, TEXT,
                    FIELDS, new HashMap<>(Map.of(KEYWORD, new HashMap<>(Map.of(TYPE, KEYWORD))
            )))));
    private static final String BOOLEAN = "boolean";
    public static final Map<String, String> BOOLEAN_MAPPING = new HashMap<>(Map.of(MappingGenerator.TYPE, BOOLEAN));
    private static final String DATE = "date";
    public static final Map<String, String> DATE_MAPPING = new HashMap<>(Map.of(MappingGenerator.TYPE, DATE));

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