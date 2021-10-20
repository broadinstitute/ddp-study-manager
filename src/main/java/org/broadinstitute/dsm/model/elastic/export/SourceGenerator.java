package org.broadinstitute.dsm.model.elastic.export;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.broadinstitute.dsm.model.NameValue;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class SourceGenerator extends BaseGenerator {

    @Override
    public Map<String, Object> generate(NameValue nameValue) {
        initializeNecessaryFields(Objects.requireNonNull(nameValue));
        Map<String, Object> mapToExport = collectExportData();
        return Map.of(DSM_OBJECT, mapToExport);
    }

    protected Map<String, Object> collectExportData() {
        Map<String, Object> result = new HashMap<>();
        String objectKey = TABLE_ALIAS_MAPPINGS.get(dbElement.getTableAlias());
        Map dynamicFieldValues;
        try {
            dynamicFieldValues = new Gson().fromJson((String) nameValue.getValue(), Map.class);
            result.put(objectKey, dynamicFieldValues);
        } catch (JsonSyntaxException jse) {
            result.put(objectKey, Map.of(dbElement.getColumnName(), nameValue.getValue()));
        }
        return result;
    }

}
