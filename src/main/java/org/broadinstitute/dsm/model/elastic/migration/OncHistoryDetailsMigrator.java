package org.broadinstitute.dsm.model.elastic.migration;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.dsm.db.OncHistoryDetail;
import org.broadinstitute.dsm.db.structure.TableName;
import org.broadinstitute.dsm.statics.ESObjectConstants;


public class OncHistoryDetailsMigrator extends BaseCollectionMigrator {

    public OncHistoryDetailsMigrator(String index, String realm) {
        super(index, realm, ESObjectConstants.ONC_HISTORY_DETAILS);
    }

    @Override
    protected Map<String, Object> getDataByRealm() {
        return (Map) OncHistoryDetail.getOncHistoryDetails(realm);
    }

}
