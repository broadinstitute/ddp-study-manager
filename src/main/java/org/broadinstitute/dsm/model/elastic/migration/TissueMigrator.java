package org.broadinstitute.dsm.model.elastic.migration;

import java.util.Map;

import org.broadinstitute.dsm.db.dao.ddp.tissue.TissueDao;

public class TissueMigrator extends BaseCollectionMigrator {

    public TissueMigrator(String index, String realm, String object) {
        super(index, realm, object);
    }

    @Override
    protected Map<String, Object> getDataByRealm() {
        return (Map) new TissueDao().getTissuesByStudy(realm);
    }
}
