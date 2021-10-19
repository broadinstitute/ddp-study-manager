package org.broadinstitute.dsm.model.elastic.export;

import java.util.Map;

import org.broadinstitute.dsm.db.structure.DBElement;
import org.broadinstitute.dsm.util.PatchUtil;

public class TestPatchUtil extends PatchUtil {

    public static Map<String, DBElement> getColumnNameMap() {
        DBElement dbElement = new DBElement("ddp_medical_record", "m", "pr", "medical_record_column");
        return Map.of("medical_record_name", dbElement);
    }

}
