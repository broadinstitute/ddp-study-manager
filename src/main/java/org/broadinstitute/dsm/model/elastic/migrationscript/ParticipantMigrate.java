package org.broadinstitute.dsm.model.elastic.migrationscript;

import org.broadinstitute.dsm.db.Participant;
import org.broadinstitute.dsm.model.elastic.Util;
import org.broadinstitute.dsm.model.elastic.export.Exportable;
import org.broadinstitute.dsm.model.elastic.export.generate.Generator;
import org.broadinstitute.dsm.model.elastic.search.ElasticSearch;
import org.broadinstitute.dsm.model.elastic.search.ElasticSearchable;
import org.broadinstitute.dsm.statics.ESObjectConstants;

import java.util.Map;

public class ParticipantMigrate implements Exportable, Generator {

    private String index;
    private String realm;
    protected final BulkExportFacade bulkExportFacade;
    private Map<String, Object> transformedObject;

    public ParticipantMigrate(String index, String realm) {
        this.index = index;
        this.realm = realm;
        this.bulkExportFacade = new BulkExportFacade(index);
    }

    @Override
    public void export() {
        Map<String, Participant> participants = Participant.getParticipants(realm);
        for (Map.Entry<String, Participant> entry: participants.entrySet()) {
            Participant participant = entry.getValue();
            String participantId = entry.getKey();
            transformedObject = Util.transformObjectToMap(Participant.class, participant);
            bulkExportFacade.addDataToRequest(generate(), getParticipantGuid(participantId, index));
        }
        bulkExportFacade.executeBulkUpsert();
    }

    @Override
    public Map<String, Object> generate() {
        return Map.of(ESObjectConstants.DSM, Map.of("participant", transformedObject));
    }
}
