package org.broadinstitute.dsm.model.elastic.export;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.broadinstitute.dsm.model.NameValue;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class SourceGenerator extends BaseGenerator {

    private Parser parser;

    public SourceGenerator(Parser parser) {
        this.parser = parser;
    }

    @Override
    public Map<String, Object> generate(NameValue nameValue) {
        initializeNecessaryFields(Objects.requireNonNull(nameValue));
        Map<String, Object> mapToExport = collectExportData();
        return Map.of(DSM_OBJECT, mapToExport);
    }

    protected Map<String, Object> collectExportData() {
        Map<String, Object> result = new HashMap<>();
        String objectKey = TABLE_ALIAS_MAPPINGS.get(dbElement.getTableAlias());
        Map<String, Object> dynamicFieldValues;
        try {
            dynamicFieldValues = new Gson().fromJson((String) nameValue.getValue(), Map.class);
            Map<String, Object> transformedMap = new HashMap<>();
            for (Map.Entry<String, Object> entry: dynamicFieldValues.entrySet()) {
                transformedMap.put(entry.getKey(), parser.parse((String) entry.getValue()));
            }
            result.put(objectKey, dynamicFieldValues);
        } catch (JsonSyntaxException jse) {
            result.put(objectKey, Map.of(dbElement.getColumnName(), nameValue.getValue()));
        }
        return result;
    }

}
