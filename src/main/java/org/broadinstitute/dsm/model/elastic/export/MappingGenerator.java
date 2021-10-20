package org.broadinstitute.dsm.model.elastic.export;

import java.util.Map;
import java.util.Objects;

import org.broadinstitute.dsm.model.NameValue;

public class MappingGenerator extends BaseGenerator {


    private final Parser typeParser;

    public MappingGenerator(Parser typeParser) {
        this.typeParser = typeParser;
    }

    @Override
    public Map<String, Object> generate(NameValue nameValue) {
        initializeNecessaryFields(Objects.requireNonNull(nameValue));
        String property = TABLE_ALIAS_MAPPINGS.get(dbElement.getTableAlias());
        String type = typeParser.parseType(nameValue);
        Map<String, Object> fieldType = Map.of("type", type);
        Map<String, Map<String, Map<String, Object>>> mappedField = Map.of(PROPERTIES, Map.of(dbElement.getColumnName(), fieldType));
        Map<String, Map<String, Map<String, Map<String, Object>>>> dsmLevelProperty = Map.of(property, mappedField);
        Map<String, Object> dsmLevelProperties = Map.of(PROPERTIES, dsmLevelProperty);
        Map<String, Map<String, Object>> dsmLevel = Map.of(DSM_OBJECT, dsmLevelProperties);
        return Map.of(PROPERTIES, dsmLevel);
    }
}
