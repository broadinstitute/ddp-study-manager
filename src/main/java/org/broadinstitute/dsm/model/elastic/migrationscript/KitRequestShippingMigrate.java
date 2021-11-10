package org.broadinstitute.dsm.model.elastic.migrationscript;

import org.broadinstitute.dsm.db.DDPInstance;
import org.broadinstitute.dsm.db.KitRequestShipping;
import org.broadinstitute.dsm.statics.ESObjectConstants;

import static org.broadinstitute.dsm.model.elastic.export.parse.TypeParser.BOOLEAN_MAPPING;
import static org.broadinstitute.dsm.model.elastic.export.parse.TypeParser.DATE_MAPPING;
import static org.broadinstitute.dsm.model.elastic.export.parse.TypeParser.TEXT_KEYWORD_MAPPING;

import java.util.List;
import java.util.Map;

public class KitRequestShippingMigrate extends BaseMigrator {

    public KitRequestShippingMigrate(String index, String realm) {
        super(index, realm, ESObjectConstants.KIT_REQUEST_SHIPPING, "dsmKitRequestId", KitRequestShipping.class);
    }

    @Override
    protected Map<String, List<Object>> getDataByRealm() {
        Map<String, List<KitRequestShipping>> kitRequests = KitRequestShipping.getAllKitRequestsByRealm(realm, null, null, true);
        return (Map) kitRequests;
    }

}
