package org.broadinstitute.dsm.model.elastic.migrationscript;


import java.util.List;
import java.util.Map;

import org.broadinstitute.dsm.db.dao.ddp.participant.ParticipantDataDao;
import org.broadinstitute.dsm.db.dto.ddp.participant.ParticipantDataDto;
import org.broadinstitute.dsm.statics.ESObjectConstants;

public class ParticipantDataMigrate extends BaseMigrator {

    private ParticipantDataDao participantDataDao;

    public ParticipantDataMigrate(String index, String realm) {
        super(index, realm, ESObjectConstants.PARTICIPANT_DATA, "participantDataId", ParticipantDataDto.class);
        participantDataDao = new ParticipantDataDao();
    }

    @Override
    protected Map<String, List<Object>> getDataByRealm() {
        return null;
    }
}
