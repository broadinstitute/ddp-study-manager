package org.broadinstitute.dsm.model.elastic.migration;

import java.util.List;
import java.util.Map;

import org.broadinstitute.dsm.db.KitRequestShipping;
import org.broadinstitute.dsm.model.elastic.export.generate.GeneratorPayload;
import org.broadinstitute.dsm.model.elastic.export.parse.Parser;
import org.broadinstitute.dsm.statics.ESObjectConstants;

public class KitRequestShippingMigrator extends BaseCollectionMigrator {

    public KitRequestShippingMigrator(String index, String realm) {
        super(index, realm, ESObjectConstants.KIT_REQUEST_SHIPPING);
    }

    @Override
    protected Map<String, Object> getDataByRealm() {
        Map<String, List<KitRequestShipping>> kitRequests = KitRequestShipping.getAllKitRequestsByRealm(realm, null, null, true);
        return (Map) kitRequests;
    }

    @Override
    public void setParser(Parser parser) {

    }

    @Override
    public void setPayload(GeneratorPayload generatorPayload) {

    }
}
