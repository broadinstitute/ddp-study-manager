package org.broadinstitute.dsm.model.elastic.export.generate;

import org.broadinstitute.dsm.model.elastic.Util;
import org.broadinstitute.dsm.model.elastic.export.parse.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

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
        Map<String, Object> returnMap = new HashMap<>(Map.of("dynamicFields", new HashMap<>(Map.of(PROPERTIES, resultMap))));
        return returnMap;
    }
}
