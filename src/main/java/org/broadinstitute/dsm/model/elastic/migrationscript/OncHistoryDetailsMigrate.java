package org.broadinstitute.dsm.model.elastic.migrationscript;

import org.broadinstitute.dsm.db.OncHistoryDetail;
import org.broadinstitute.dsm.statics.ESObjectConstants;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.broadinstitute.dsm.model.elastic.export.parse.TypeParser.*;
import static org.broadinstitute.dsm.model.elastic.export.parse.TypeParser.TEXT_KEYWORD_MAPPING;

public class OncHistoryDetailsMigrate extends BaseMigrator {


    public OncHistoryDetailsMigrate(String index, String realm) {
        super(index, realm, ESObjectConstants.ONC_HISTORY_DETAIL_RECORDS, "");
    }

    @Override
    protected Map<String, List<Object>> getDataByRealm() {
        return (Map) OncHistoryDetail.getOncHistoryDetails(realm);
    }

}
