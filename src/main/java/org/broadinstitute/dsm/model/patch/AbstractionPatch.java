package org.broadinstitute.dsm.model.patch;

import org.broadinstitute.ddp.handlers.util.Result;
import org.broadinstitute.dsm.db.structure.DBElement;
import org.broadinstitute.dsm.model.AbstractionWrapper;
import org.broadinstitute.dsm.model.NameValue;
import org.broadinstitute.dsm.model.Patch;
import spark.utils.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class AbstractionPatch extends BasePatch {

    String primaryKeyId;

    @Override
    protected Object patchNameValuePairs() {
        return null;
    }

    @Override
    public Object patchNameValuePair() {
        return null;

    }

    @Override
    Object handleSingleNameValue(DBElement dbElement) {
        String primaryKeyId = AbstractionWrapper.createNewAbstractionFieldValue(patch.getParentId(), patch.getFieldId(), patch.getUser(), patch.getNameValue(), dbElement);
        return Map.of(PRIMARY_KEY_ID, primaryKeyId);
    }

    @Override
    Optional<Object> processEachNameValue(NameValue nameValue, DBElement dbElement) {
        if (StringUtils.isBlank(primaryKeyId)) {
            primaryKeyId = AbstractionWrapper.createNewAbstractionFieldValue(patch.getParentId(), patch.getFieldId(), patch.getUser(), nameValue, dbElement);
        }
        if (!Patch.patch(primaryKeyId, patch.getUser(), nameValue, dbElement)) {
            throw new RuntimeException("An error occurred while attempting to patch ");
        }
        return Optional.ofNullable(primaryKeyId);
    }
}
