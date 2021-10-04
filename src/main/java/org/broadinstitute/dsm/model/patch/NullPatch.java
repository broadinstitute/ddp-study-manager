package org.broadinstitute.dsm.model.patch;

import java.util.List;
import java.util.Optional;

import org.broadinstitute.dsm.db.structure.DBElement;
import org.broadinstitute.dsm.model.NameValue;

public class NullPatch extends BasePatch {

    @Override
    Optional<Object> processSingleNameValue(NameValue nameValue, DBElement dbElement) {
        return Optional.empty();
    }

    @Override
    Optional<Object> processMultipleNameValues(List<NameValue> nameValues) {
        return Optional.empty();
    }
}
