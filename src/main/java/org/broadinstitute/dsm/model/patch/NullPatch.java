package org.broadinstitute.dsm.model.patch;

import java.util.List;
import java.util.Optional;

import org.broadinstitute.dsm.db.structure.DBElement;
import org.broadinstitute.dsm.model.NameValue;

public class NullPatch extends BasePatch {

    @Override
    protected Object patchNameValuePairs() {
        return new Object();
    }

    @Override
    public Object patchNameValuePair() {
        return new Object();
    }

    @Override
    Optional<Object> processEachNameValue(NameValue nameValue, DBElement dbElement) {
        return Optional.empty();
    }

    @Override
    Object handleSingleNameValue(DBElement dbElement) {
        return new Object();
    }
}
