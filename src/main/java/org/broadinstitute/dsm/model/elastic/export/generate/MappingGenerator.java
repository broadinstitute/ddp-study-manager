package org.broadinstitute.dsm.model.elastic.export.generate;

import org.broadinstitute.dsm.model.elastic.Util;
import org.broadinstitute.dsm.model.elastic.export.parse.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

abstract public class MappingGenerator extends BaseGenerator {

    private static final Logger logger = LoggerFactory.getLogger(MappingGenerator.class);

    public static final String TYPE = "type";
    public static final String NESTED = "nested";
    public static final String TYPE_KEYWORD = "keyword";

    public MappingGenerator(Parser parser, GeneratorPayload generatorPayload) {
        super(parser, generatorPayload);
    }

    public MappingGenerator() {}

    @Override
    public Map<String, Object> generate() {
        logger.info("preparing mapping to upsert");
        return getCompleteMap(construct());
    }

    public Map<String, Object> getCompleteMap(Object propertyMap) {
        String propertyName = getPropertyName();
        Map<String, Object> objectLevel = new HashMap<>(Map.of(propertyName, propertyMap));
        Map<String, Object> dsmLevelProperties = new HashMap<>(Map.of(PROPERTIES, objectLevel));
        Map<String, Map<String, Object>> dsmLevel = new HashMap<>(Map.of(DSM_OBJECT, dsmLevelProperties));
        return new HashMap<>(Map.of(PROPERTIES, dsmLevel));
    }


    @Override
    protected Map<String, Object> parseJson() {
        Map<String, Object> resultMap = new HashMap<>();
        Map<String, Object> fieldsByValues = parseJsonToMapFromValue();
        for (Map.Entry<String, Object> entry: fieldsByValues.entrySet()) {
            Object eachType = parser.parse(String.valueOf(entry.getValue()));
            resultMap.put(Util.underscoresToCamelCase(entry.getKey()), eachType);
        }
        return new HashMap<>(Map.of(Util.underscoresToCamelCase(getDBElement().getColumnName()),
                new HashMap<>(Map.of(PROPERTIES,
                resultMap))));
    }

    @Override
    public Map<String, Object> merge(Map<String, Object> base, Map<String, Object> toMerge) {
        String propertyName = Util.underscoresToCamelCase(getDBElement().getColumnName());
//        base.putIfAbsent(propertyName, toMerge);



        //dsm -> properties -> medicalRecords -> properties -> mrRecordId: {}, ragaca: {}, additionalValuesjson: { properties: {key: {type = ...}
        getFieldLevel(base).putAll(getFieldLevel(toMerge));
        return base;
    }

    private Map<String, Object> getFieldLevel(Map<String, Object> topLevel) {
        if (topLevel.isEmpty()) {
            return topLevel;
        }
        String propertyName = Util.underscoresToCamelCase(getDBElement().getColumnName());
//        Map outerPropertyMap = (Map)  topLevel.get(PROPERTIES);
        Map innerPropertyMap = (Map) topLevel.get(propertyName);
        Map innerProperty = (Map) innerPropertyMap.get(PROPERTIES);
        if (innerProperty != null)
            return innerProperty;
        else
            return innerPropertyMap;
    }

}
