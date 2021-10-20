package org.broadinstitute.dsm.model.elastic.export;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.broadinstitute.dsm.db.structure.DBElement;
import org.broadinstitute.dsm.model.NameValue;
import org.broadinstitute.dsm.util.PatchUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class SourceGenerator implements ValueGenerator {

    public static final String DSM_OBJECT = "dsm";
    public static final Map<String, String> TABLE_ALIAS_MAPPINGS = Map.of(
            "m", "medicalRecords",
            "t", "tissueRecords",
            "oD", "oncHistoryDetailRecords",
            "r", "participant",
            "p", "participant",
            "d", "participant"
    );
    protected NameValue nameValue;
    protected DBElement dbElement;

    @Override
    public Map<String, Object> generate(NameValue nameValue) {
        initializeNecessaryFields(Objects.requireNonNull(nameValue));
        Map<String, Object> mapToExport = collectExportData();
        return Map.of(DSM_OBJECT, mapToExport);
    }

    protected void initializeNecessaryFields(NameValue nameValue) {
        this.nameValue = nameValue;
        dbElement = getDBElement();
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

    protected DBElement getDBElement() {
        return PatchUtil.getColumnNameMap().get(Objects.requireNonNull(nameValue).getName());
    }
}
