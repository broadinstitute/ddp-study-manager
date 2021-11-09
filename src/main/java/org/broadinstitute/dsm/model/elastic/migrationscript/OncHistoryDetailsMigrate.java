package org.broadinstitute.dsm.model.elastic.migrationscript;

import org.broadinstitute.dsm.db.OncHistoryDetail;
import org.broadinstitute.dsm.statics.ESObjectConstants;

import java.util.List;
import java.util.Map;


public class OncHistoryDetailsMigrate extends BaseMigrator {


    public OncHistoryDetailsMigrate(String index, String realm) {
        super(index, realm, ESObjectConstants.ONC_HISTORY_DETAIL_RECORDS, "oncHistoryDetailId", OncHistoryDetail.class);
    }

    @Override
    protected Map<String, List<Object>> getDataByRealm() {
        return (Map) OncHistoryDetail.getOncHistoryDetails(realm);
    }

}
