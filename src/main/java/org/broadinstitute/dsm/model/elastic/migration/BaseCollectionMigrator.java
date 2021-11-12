package org.broadinstitute.dsm.model.elastic.migration;

import java.util.List;
import java.util.Map;

import org.broadinstitute.dsm.model.elastic.Util;
import org.broadinstitute.dsm.statics.ESObjectConstants;

public abstract class BaseCollectionMigrator extends BaseMigrator {

    protected String primaryId;
    protected List<Map<String, Object>> transformedList;

    public BaseCollectionMigrator(String index, String realm, String object, String primaryId) {
        super(index, realm, object);
        this.primaryId = primaryId;
    }

    @Override
    public Map<String, Object> generate() {
        return Map.of(ESObjectConstants.DSM, Map.of(object, transformedList));
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
