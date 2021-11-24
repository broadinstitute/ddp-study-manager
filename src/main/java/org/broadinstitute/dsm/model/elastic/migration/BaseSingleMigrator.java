package org.broadinstitute.dsm.model.elastic.migration;

import java.util.Map;

import org.broadinstitute.dsm.model.elastic.Util;
import org.broadinstitute.dsm.statics.ESObjectConstants;

public abstract class BaseSingleMigrator extends BaseMigrator {

    private Map<String, Object> transformedObject;

    public BaseSingleMigrator(String index, String realm, String object) {
        super(index, realm, object);
    }

    @Override
    public Map<String, Object> generate() {
        return Map.of(ESObjectConstants.DSM, Map.of(object, transformedObject));
    }

    @Override
    protected void transformObject(Object object) {
        transformedObject = Util.transformObjectToMap(object);
    }
}