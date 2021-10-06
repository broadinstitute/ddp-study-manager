package org.broadinstitute.dsm.model.patch;

import java.util.Optional;

import org.broadinstitute.dsm.db.structure.DBElement;
import org.broadinstitute.dsm.model.NameValue;
import org.broadinstitute.dsm.model.Patch;

public class TissuePatch extends BasePatch {

    public TissuePatch(Patch patch) {
        super(patch);
    }

    @Override
    protected Object patchNameValuePairs() {
        return null;
    }

    @Override
    protected Object patchNameValuePair() {
        return null;
    }

    @Override
    Object handleSingleNameValue(DBElement dbElement) {
        return null;
    }

    @Override
    Optional<Object> processEachNameValue(NameValue nameValue, DBElement dbElement) {
        return Optional.empty();
    }
}
