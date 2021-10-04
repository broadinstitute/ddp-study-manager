package org.broadinstitute.dsm.model.patch;

import java.util.List;
import java.util.Optional;

import org.broadinstitute.dsm.db.structure.DBElement;
import org.broadinstitute.dsm.model.NameValue;
import org.broadinstitute.dsm.model.Patch;
import org.broadinstitute.dsm.util.PatchUtil;


public abstract class BasePatch implements Patchable {

    protected Patch patch;

    public BasePatch() {

    }

    public BasePatch(Patch patch) {
        this.patch = patch;
    }

    abstract Optional<Object> processSingleNameValue(NameValue nameValue, DBElement dbElement);

    Optional<Object> processMultipleNameValues(List<NameValue> nameValues) {
        Optional<Object> result = Optional.empty();
        for (NameValue nameValue : patch.getNameValues()) {
            DBElement dbElement = PatchUtil.getColumnNameMap().get(nameValue.getName());
            if (dbElement != null) {
                result = processSingleNameValue(nameValue, dbElement);
            }
            else {
                throw new RuntimeException("DBElement not found in ColumnNameMap: " + nameValue.getName());
            }
        }
        return result;
    }

    private boolean hasQuestion(NameValue nameValue) {
        return nameValue.getName().contains("question");
    }





}
