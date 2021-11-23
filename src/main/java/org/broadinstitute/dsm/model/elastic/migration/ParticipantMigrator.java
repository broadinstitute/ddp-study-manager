package org.broadinstitute.dsm.model.elastic.migration;

import java.util.Map;

import org.broadinstitute.dsm.db.Participant;
import org.broadinstitute.dsm.model.elastic.Util;
import org.broadinstitute.dsm.model.elastic.export.Exportable;
import org.broadinstitute.dsm.model.elastic.export.generate.Generator;
import org.broadinstitute.dsm.model.elastic.export.generate.GeneratorPayload;
import org.broadinstitute.dsm.model.elastic.export.parse.Parser;
import org.broadinstitute.dsm.statics.ESObjectConstants;

public class ParticipantMigrator extends BaseSingleMigrator implements Exportable, Generator {

    public ParticipantMigrator(String index, String realm) {
        super(index, realm, ESObjectConstants.PARTICIPANT);
    }

    @Override
    protected Map<String, Object> getDataByRealm() {
        return (Map) Participant.getParticipants(realm);
    }

    @Override
    public void setParser(Parser parser) {

    }

    @Override
    public void setPayload(GeneratorPayload generatorPayload) {

    }
}
