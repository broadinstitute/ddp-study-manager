package org.broadinstitute.dsm.model.elastic.migrationscript;

import org.broadinstitute.dsm.model.elastic.Util;

import java.util.List;
import java.util.Map;

public abstract class BaseCollectionMigrator extends BaseMigrator {

    public BaseCollectionMigrator(String index, String realm, String object, String primaryId, Class aClass) {
        super(index, realm, object, primaryId, aClass);
    }

    @Override
    protected void transformObject(Object object) {
        transformedList = Util.transformObjectCollectionToCollectionMap((List) object);
        setPrimaryId();
    }

    private void setPrimaryId() {
        for(Map<String, Object> map: transformedList) {
            map.put(Util.ID, map.get(primaryId));
        }
    }
}
