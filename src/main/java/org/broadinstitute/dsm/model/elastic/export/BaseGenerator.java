package org.broadinstitute.dsm.model.elastic.export;

import org.broadinstitute.dsm.db.structure.DBElement;
import org.broadinstitute.dsm.model.NameValue;
import org.broadinstitute.dsm.util.PatchUtil;

import java.util.Map;
import java.util.Objects;

public abstract class BaseGenerator implements Generator {


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

    protected void initializeNecessaryFields(NameValue nameValue) {
        this.nameValue = nameValue;
        dbElement = getDBElement();
    }

    protected DBElement getDBElement() {
        return PatchUtil.getColumnNameMap().get(Objects.requireNonNull(nameValue).getName());
    }
}
