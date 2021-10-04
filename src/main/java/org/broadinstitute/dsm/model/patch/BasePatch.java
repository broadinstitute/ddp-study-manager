package org.broadinstitute.dsm.model.patch;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.broadinstitute.dsm.db.structure.DBElement;
import org.broadinstitute.dsm.model.NameValue;
import org.broadinstitute.dsm.model.Patch;
import org.broadinstitute.dsm.util.NotificationUtil;
import org.broadinstitute.dsm.util.PatchUtil;


public abstract class BasePatch implements Patchable {

    protected static final String PARTICIPANT_ID = "participantId";
    protected static final String PRIMARY_KEY_ID = "primaryKeyId";
    protected static final String NAME_VALUE = "NameValue";
    protected static final String STATUS = "status";

    protected Patch patch;

    public BasePatch() {
    }

    public BasePatch(Patch patch) {
        this.patch = patch;
    }

    abstract Optional<NameValue> processSingleNameValue(NameValue nameValue, DBElement dbElement);

    List<NameValue> processMultipleNameValues(List<NameValue> nameValues) {
        List<NameValue> updatedNameValues = new ArrayList<>();
        for (NameValue nameValue : patch.getNameValues()) {
            DBElement dbElement = PatchUtil.getColumnNameMap().get(nameValue.getName());
            if (dbElement != null) {
                processSingleNameValue(nameValue, dbElement).ifPresent(updatedNameValues::add);
            }
            else {
                throw new RuntimeException("DBElement not found in ColumnNameMap: " + nameValue.getName());
            }
        }
        return updatedNameValues;
    }

    protected boolean hasQuestion(NameValue nameValue) {
        return nameValue.getName().contains("question");
    }




}
