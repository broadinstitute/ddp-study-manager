package org.broadinstitute.dsm.model.elastic.export;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.broadinstitute.dsm.model.NameValue;

public class MappingGenerator extends BaseGenerator {


    private final Parser parser;

    public MappingGenerator(Parser parser) {
        this.parser = parser;
    }

    @Override
    public Map<String, Object> generate(NameValue nameValue) {
        initializeNecessaryFields(Objects.requireNonNull(nameValue));
        String property = TABLE_ALIAS_MAPPINGS.get(dbElement.getTableAlias());
        Map<String, Map<String, Object>> field = new HashMap<>();
        try {
            Map<String, Object> fieldsByValues = new Gson().fromJson((String) nameValue.getValue(), Map.class);
            for (Map.Entry<String, Object> entry: fieldsByValues.entrySet()) {
                Object eachType = parser.parse((String) entry.getValue());
                field.put(entry.getKey(), Map.of("type", eachType));
            }
        } catch (JsonSyntaxException jse) {
            Object type = parser.parse((String)nameValue.getValue());
            field = Map.of(dbElement.getColumnName(), Map.of("type", type)); // ddp_medical_column : {type: "text"}
        }
        Map<String, Map<String, Map<String, Object>>> mappedField = Map.of(PROPERTIES, field); //properties
        Map<String, Map<String, Map<String, Map<String, Object>>>> dsmLevelProperty = Map.of(property, mappedField); //medicalRecords
        Map<String, Object> dsmLevelProperties = Map.of(PROPERTIES, dsmLevelProperty); //properties
        Map<String, Map<String, Object>> dsmLevel = Map.of(DSM_OBJECT, dsmLevelProperties); //dsm
        return Map.of(PROPERTIES, dsmLevel); //properties
    }
}
