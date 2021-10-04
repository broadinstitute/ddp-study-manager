package org.broadinstitute.dsm.model.patch;

import java.util.List;
import java.util.Optional;

import org.broadinstitute.dsm.db.structure.DBElement;
import org.broadinstitute.dsm.model.NameValue;

public class NullPatch extends BasePatch {

    @Override
    public Object doPatch() {
        return new Object();
    }

    @Override
    Optional<NameValue> processEachNameValue(NameValue nameValue, DBElement dbElement) {
        return Optional.empty();
    }

    @Override
    List<NameValue> processMultipleNameValues(List<NameValue> nameValues) {
        return List.of();
    }
}
