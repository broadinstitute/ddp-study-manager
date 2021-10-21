package org.broadinstitute.dsm.model.elastic.export;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.broadinstitute.dsm.model.NameValue;

public class MappingGenerator extends BaseGenerator {


    public static final String TYPE = "type";

    public MappingGenerator(Parser parser) {
        super(parser);
    }


    @Override
    public Map<String, Object> generate(NameValue nameValue) {
        initializeNecessaryFields(Objects.requireNonNull(nameValue));
        String property = getOuterPropertyByAlias();
        Map<String, Object> field = collect();
        Map<String, Object> mappedField = Map.of(PROPERTIES, field);
        Map<String, Object> dsmLevelProperty = Map.of(property, mappedField);
        Map<String, Object> dsmLevelProperties = Map.of(PROPERTIES, dsmLevelProperty);
        Map<String, Map<String, Object>> dsmLevel = Map.of(DSM_OBJECT, dsmLevelProperties);
        return Map.of(PROPERTIES, dsmLevel);
    }

    @Override
    protected Map<String, Object> parseJson() {
        Map<String, Object> resultMap = new HashMap<>();
        Map<String, Object> fieldsByValues = GSON.fromJson((String) nameValue.getValue(), Map.class);
        for (Map.Entry<String, Object> entry: fieldsByValues.entrySet()) {
            Object eachType = parser.parse((String) entry.getValue());
            resultMap.put(entry.getKey(), Map.of(MappingGenerator.TYPE, eachType));
        }
        return resultMap;
    }

    @Override
    protected Map<String, Object> parseSingleElement() {
        Object type = parser.parse((String) nameValue.getValue());
        Map<String, Object> columnWithType = Map.of(dbElement.getColumnName(), Map.of(MappingGenerator.TYPE, type));
        return columnWithType;
    }

}
