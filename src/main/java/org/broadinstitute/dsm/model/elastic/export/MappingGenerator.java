package org.broadinstitute.dsm.model.elastic.export;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.broadinstitute.dsm.model.NameValue;

public class MappingGenerator extends BaseGenerator {


    public static final String TYPE = "type";
    private final Parser parser;

    public MappingGenerator(Parser parser) {
        this.parser = parser;
    }

    @Override
    public Map<String, Object> generate(NameValue nameValue) {
        initializeNecessaryFields(Objects.requireNonNull(nameValue));
        String property = TABLE_ALIAS_MAPPINGS.get(dbElement.getTableAlias());
        Map<String, Map<String, Object>> field = collectMapping(nameValue);
        Map<String, Map<String, Map<String, Object>>> mappedField = Map.of(PROPERTIES, field);
        Map<String, Map<String, Map<String, Map<String, Object>>>> dsmLevelProperty = Map.of(property, mappedField);
        Map<String, Object> dsmLevelProperties = Map.of(PROPERTIES, dsmLevelProperty);
        Map<String, Map<String, Object>> dsmLevel = Map.of(DSM_OBJECT, dsmLevelProperties);
        return Map.of(PROPERTIES, dsmLevel);
    }

    private Map<String, Map<String, Object>> collectMapping(NameValue nameValue) {
        Map<String, Map<String, Object>> field = new HashMap<>();
        try {
            Map<String, Object> fieldsByValues = new Gson().fromJson((String) nameValue.getValue(), Map.class);
            for (Map.Entry<String, Object> entry: fieldsByValues.entrySet()) {
                Object eachType = parser.parse((String) entry.getValue());
                field.put(entry.getKey(), Map.of(TYPE, eachType));
            }
        } catch (JsonSyntaxException jse) {
            Object type = parser.parse((String) nameValue.getValue());
            field = Map.of(dbElement.getColumnName(), Map.of(TYPE, type));
        }
        return field;
    }
}
