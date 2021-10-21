package org.broadinstitute.dsm.model.elastic.export;

import org.broadinstitute.dsm.model.NameValue;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class SourceGenerator extends BaseGenerator {


    public SourceGenerator(Parser parser, GeneratorPayload generatorPayload) {
        super(parser);

    }


    @Override
    public Map<String, Object> generate() {
        initializeNecessaryFields(Objects.requireNonNull(nameValue));
        Map<String, Object> mapToExport = collect();
        return Map.of(DSM_OBJECT, mapToExport);
    }

    @Override
    protected Map<String, Object> parseJson() {
        Map<String, Object> dynamicFieldValues = GSON.fromJson((String) nameValue.getValue(), Map.class);
        Map<String, Object> transformedMap = new HashMap<>();
        for (Map.Entry<String, Object> entry : dynamicFieldValues.entrySet()) {
            transformedMap.put(entry.getKey(), parser.parse((String) entry.getValue()));
        }
        PropertyInfo outerPropertyByAlias = getOuterPropertyByAlias();
        Map.of(getOuterPropertyByAlias(), transformedMap);
        return null;
    }

    @Override
    protected Map<String, Object> parseSingleElement() {
        PropertyInfo outerPropertyByAlias = getOuterPropertyByAlias();
        Map<String, Object> propertyWithFieldValuePair =
                Map.of(getOuterPropertyByAlias(), Map.of(dbElement.getColumnName(), parser.parse((String) nameValue.getValue())));
        return propertyWithFieldValuePair;
    }

}
