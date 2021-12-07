package org.broadinstitute.dsm.model.elastic.migration;

import org.broadinstitute.dsm.db.dao.ddp.tissue.TissueDao;
import org.broadinstitute.dsm.statics.ESObjectConstants;

import java.util.Map;

public class TissueMigrator extends BaseCollectionMigrator {

    public TissueMigrator(String index, String realm) {
        super(index, realm, ESObjectConstants.TISSUE);
    }

    @Override
    protected Map<String, Object> getDataByRealm() {
        return (Map) new TissueDao().getTissuesByStudy(realm);
    }

}
