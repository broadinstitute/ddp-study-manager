package org.broadinstitute.dsm.model.elastic.export;

import java.util.HashMap;
import java.util.Map;

import org.broadinstitute.dsm.model.elastic.Util;

public class MappingGenerator extends BaseGenerator {


    public static final String TYPE = "type";
    public static final String NESTED = "nested";
    public static final String TYPE_KEYWORD = "keyword";

    public MappingGenerator(Parser parser, GeneratorPayload generatorPayload) {
        super(parser, generatorPayload);
    }


    @Override
    public Map<String, Object> generate() {
        PropertyInfo propertyInfo = getOuterPropertyByAlias();
        String propertyName = propertyInfo.getPropertyName();
        Map<String, Object> mappedField = buildMappedField();
        Map<String, Object> dsmLevelProperty = Map.of(propertyName, mappedField);
        Map<String, Object> dsmLevelProperties = Map.of(PROPERTIES, dsmLevelProperty);
        Map<String, Map<String, Object>> dsmLevel = Map.of(DSM_OBJECT, dsmLevelProperties);
        return Map.of(PROPERTIES, dsmLevel);
    }

    private Map<String, Object> buildMappedField() {
        PropertyInfo propertyInfo = getOuterPropertyByAlias();
        boolean isPropertyCollection = propertyInfo.isCollection();
        Map<String, Object> field = collect();
        Map<String, Object> mappedField;
        if (isPropertyCollection) {
            mappedField = Map.of(TYPE, NESTED, PROPERTIES, field);
        } else {
            mappedField = Map.of(PROPERTIES, field);
        }
        return mappedField;
    }

    @Override
    protected Map<String, Object> parseJson() {
        Map<String, Object> resultMap = new HashMap<>();
        Map<String, Object> fieldsByValues = GSON.fromJson((String) getNameValue().getValue(), Map.class);
        for (Map.Entry<String, Object> entry: fieldsByValues.entrySet()) {
            Object eachType = parser.parse((String) entry.getValue());
            resultMap.put(entry.getKey(), Map.of(MappingGenerator.TYPE, eachType));
        }
        return resultMap;
    }

    @Override
    protected Map<String, Object> parseSingleElement() {
        return getFieldWithElement();
    }

    @Override
    protected Map<String, Object> getElementWithId(Object type) {
        return Map.of(
                ID, Map.of(TYPE, TYPE_KEYWORD),
                getDBElement().getColumnName(), Map.of(MappingGenerator.TYPE, type)
        );
    }

    @Override
    protected Map<String, Object> getElement(Object type) {
        return Map.of(getDBElement().getColumnName(), Map.of(MappingGenerator.TYPE, type));
    }

}
