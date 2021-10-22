package org.broadinstitute.dsm.model.elastic.export;

import org.broadinstitute.dsm.db.structure.DBElement;
import org.broadinstitute.dsm.util.PatchUtil;

import java.util.Map;

class TestPatchUtil extends PatchUtil {

    public static final String MEDICAL_RECORD_COLUMN = "medical_record_column";

    public static Map<String, DBElement> getColumnNameMap() {
        DBElement dbElement = new DBElement("ddp_medical_record", "m", "pr", MEDICAL_RECORD_COLUMN);
        return Map.of(
                MEDICAL_RECORD_COLUMN, dbElement
        );
    }

}
