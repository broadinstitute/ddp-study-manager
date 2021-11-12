package org.broadinstitute.dsm.model.elastic.migration;

import java.util.Map;

import org.broadinstitute.dsm.db.Participant;
import org.broadinstitute.dsm.model.elastic.Util;
import org.broadinstitute.dsm.model.elastic.export.Exportable;
import org.broadinstitute.dsm.model.elastic.export.generate.Generator;
import org.broadinstitute.dsm.statics.ESObjectConstants;

public class ParticipantMigrator extends BaseMigrator implements Exportable, Generator {

    private String realm;
    protected final BulkExportFacade bulkExportFacade;
    private Map<String, Object> transformedObject;

    public ParticipantMigrator(String index, String realm) {
        super(index, realm, ESObjectConstants.PARTICIPANT);
        this.realm = realm;
        this.bulkExportFacade = new BulkExportFacade(index);
    }

    @Override
    public Map<String, Object> generate() {
        return Map.of(ESObjectConstants.DSM, Map.of(object, transformedObject));
    }

    @Override
    protected void transformObject(Object object) {
        transformedObject = Util.transformObjectToMap(object);
    }

    @Override
    protected Map<String, Object> getDataByRealm() {
        return (Map) Participant.getParticipants(realm);

    }
}
