package org.broadinstitute.dsm.model.elastic.migrationscript;

import org.broadinstitute.dsm.db.Participant;
import org.broadinstitute.dsm.model.elastic.Util;
import org.broadinstitute.dsm.model.elastic.export.Exportable;
import org.broadinstitute.dsm.model.elastic.search.ElasticSearch;
import org.broadinstitute.dsm.model.elastic.search.ElasticSearchable;

import java.util.Map;

public class ParticipantMigrate implements Exportable {

    private String index;
    private String realm;
    private final ElasticSearchable elasticSearch;
    protected final BulkExportFacade bulkExportFacade;
    
    public ParticipantMigrate(String index, String realm) {
        this.index = index;
        this.realm = realm;
        this.bulkExportFacade = new BulkExportFacade(index);
        this.elasticSearch = new ElasticSearch();
    }

    @Override
    public void export() {
        Map<String, Participant> participants = Participant.getParticipants(realm);
        Map<String, Object> transformedObject = Util.transformObjectToMap(Participant.class, participants);
        bulkExportFacade.


    }
}
