package org.broadinstitute.dsm.model.elastic.export.generate;

import org.broadinstitute.dsm.model.elastic.Util;
import org.broadinstitute.dsm.model.elastic.export.parse.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

abstract public class MappingGenerator extends BaseGenerator implements Merger {

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
        String propertyName = getOuterPropertyByAlias().getPropertyName();
        Map<String, Object> objectLevel = Map.of(propertyName, construct());
        Map<String, Object> dsmLevelProperties = Map.of(PROPERTIES, objectLevel);
        Map<String, Map<String, Object>> dsmLevel = Map.of(DSM_OBJECT, dsmLevelProperties);
        return Map.of(PROPERTIES, dsmLevel);
    }


    @Override
    protected Map<String, Object> parseJson() {
        Map<String, Object> resultMap = new HashMap<>();
        Map<String, Object> fieldsByValues = parseJsonToMapFromValue();
        for (Map.Entry<String, Object> entry: fieldsByValues.entrySet()) {
            Object eachType = parser.parse(String.valueOf(entry.getValue()));
            resultMap.put(Util.underscoresToCamelCase(entry.getKey()), eachType);
        }
        return Map.of(Util.underscoresToCamelCase(getDBElement().getColumnName()), resultMap);
    }

    @Override
    public Map<String, Object> merge(Map<String, Object> base, Map<String, Object> toMerge) {
        base.putIfAbsent(PROPERTIES, toMerge.get(PROPERTIES));
        getFieldLevel(base).putAll(getFieldLevel(toMerge));
        return base;
    }

    private Map<String, Object> getFieldLevel(Map<String, Object> topLevel) {
        String propertyName = getOuterPropertyByAlias().getPropertyName();
        return ((Map)((Map)((Map)((Map)((Map)topLevel.get(PROPERTIES))
                .get(DSM_OBJECT))
                .get(PROPERTIES))
                .get(propertyName))
                .get(PROPERTIES));
    }

}
