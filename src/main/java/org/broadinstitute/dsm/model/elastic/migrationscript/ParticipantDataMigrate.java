package org.broadinstitute.dsm.model.elastic.migrationscript;


import org.broadinstitute.dsm.db.OncHistoryDetail;
import org.broadinstitute.dsm.db.ParticipantData;
import org.broadinstitute.dsm.statics.ESObjectConstants;

import java.util.List;
import java.util.Map;

public class ParticipantDataMigrate extends BaseMigrator {

    public ParticipantDataMigrate(String index, String realm) {
        super(index, realm, ESObjectConstants.PARTICIPANT_DATA, "participantDataId", ParticipantData.class);
    }

    @Override
    protected Map<String, List<Object>> getDataByRealm() {
        return null;
    }
}
