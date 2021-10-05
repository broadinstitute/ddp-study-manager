package org.broadinstitute.dsm.model.patch;

import org.broadinstitute.dsm.db.structure.DBElement;
import org.broadinstitute.dsm.model.AbstractionWrapper;
import org.broadinstitute.dsm.model.NameValue;
import org.broadinstitute.dsm.model.Patch;
import spark.utils.StringUtils;

import java.util.Optional;

public class AbstractionPatch extends BasePatch {

    String primaryKeyId;

    @Override
    public Object doPatch() {
        return null;
    }

    @Override
    Object handleSingleNameValue(DBElement dbElement) {
        return null;
    }

    @Override
    Optional<NameValue> processEachNameValue(NameValue nameValue, DBElement dbElement) {
        if (StringUtils.isBlank(primaryKeyId)) {
            primaryKeyId = AbstractionWrapper.createNewAbstractionFieldValue(patch.getParentId(), patch.getFieldId(), patch.getUser(), nameValue, dbElement);
        }
        if (!Patch.patch(primaryKeyId, patch.getUser(), nameValue, dbElement)) {
            throw new RuntimeException("An error occurred while attempting to patch ");
        }
        return Optional.empty();
    }
}
