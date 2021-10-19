package org.broadinstitute.dsm.model.elastic.export;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import org.broadinstitute.dsm.db.structure.DBElement;
import org.broadinstitute.dsm.model.NameValue;
import org.broadinstitute.dsm.util.PatchUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class SourceGenerator implements Generator {

    public static final String DSM_OBJECT = "dsm";
    public static final Map<String, String> TABLE_ALIAS_MAPPINGS = Map.of(
            "m", "medicalRecords",
            "t", "tissueRecords",
            "oD", "oncHistoryDetailRecords",
            "r", "participant",
            "p", "participant",
            "d", "participant"
    );

    @Override
    public Map<String, Object> generate(NameValue nameValue) {
        DBElement dbElement = getDBElement(nameValue);

        Map<String, Object> result = new HashMap<>();
        String objectKey = TABLE_ALIAS_MAPPINGS.get(dbElement.getTableAlias());
        result.put(objectKey, Map.of(dbElement.getColumnName(), nameValue.getValue()));
        Map map = new Gson().fromJson((String) nameValue.getValue(), Map.class);
//        if (DBElement.JSON_TYPE_COLUMNS.contains(dbElement.getColumnName())) {
//            Map<String, Object> nestedMap = Map.of(nameValue.getName(), nameValue.getValue());
//            result.put(objectKey, Map.of(dbElement.getColumnName(), nestedMap));
//        } else {
//        }

        return Map.of(DSM_OBJECT, result);
    }

    protected DBElement getDBElement(NameValue nameValue) {
        return PatchUtil.getColumnNameMap().get(Objects.requireNonNull(nameValue).getName());
    }
}
