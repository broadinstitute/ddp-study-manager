package org.broadinstitute.dsm.model.elastic.migrationscript;

import org.broadinstitute.dsm.db.KitRequestShipping;
import org.broadinstitute.dsm.statics.ESObjectConstants;

import java.util.List;
import java.util.Map;

public class KitRequestShippingMigrate extends BaseCollectionMigrator {

    public KitRequestShippingMigrate(String index, String realm) {
        super(index, realm, ESObjectConstants.KIT_REQUEST_SHIPPING, "dsmKitRequestId");
    }

    @Override
    protected Map<String, Object> getDataByRealm() {
        Map<String, List<KitRequestShipping>> kitRequests = KitRequestShipping.getAllKitRequestsByRealm(realm, null, null, true);
        return (Map) kitRequests;
    }

}
