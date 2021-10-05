package org.broadinstitute.dsm.model.patch;

import org.broadinstitute.dsm.db.structure.DBElement;
import org.broadinstitute.dsm.model.AbstractionWrapper;
import org.broadinstitute.dsm.model.NameValue;
import org.broadinstitute.dsm.model.Patch;
import spark.utils.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class AbstractionPatch extends BasePatch {

    String primaryKeyId;
    public static final int FIRST_PRIMARY_KEY_ID = 0;
    private static final Map<String, Object> NULL_KEY;

    static {
        NULL_KEY = new HashMap<>();
        NULL_KEY.put(PRIMARY_KEY_ID, null);
    }

    public AbstractionPatch(Patch patch) {
        super(patch);
    }

    @Override
    protected Object patchNameValuePairs() {
        List<Object> firstPrimaryKey = processMultipleNameValues();
        return firstPrimaryKey.isEmpty() ? NULL_KEY : firstPrimaryKey.get(FIRST_PRIMARY_KEY_ID);
    }

    @Override
    public Object patchNameValuePair() {
        Optional<Object> maybeMap = processSingleNameValue();
        return maybeMap.orElse(NULL_KEY);
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
