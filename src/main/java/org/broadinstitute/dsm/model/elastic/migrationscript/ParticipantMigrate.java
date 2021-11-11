package org.broadinstitute.dsm.model.elastic.migrationscript;

import java.util.Map;

import org.broadinstitute.dsm.db.Participant;
import org.broadinstitute.dsm.model.elastic.Util;
import org.broadinstitute.dsm.model.elastic.export.Exportable;
import org.broadinstitute.dsm.model.elastic.export.generate.Generator;
import org.broadinstitute.dsm.statics.ESObjectConstants;

public class ParticipantMigrate extends BaseMigrator implements Exportable, Generator {

    private String index;
    private String realm;
    protected final BulkExportFacade bulkExportFacade;
    private Map<String, Object> transformedObject;

    public ParticipantMigrate(String index, String realm) {
        super(index, realm, "participant");
        this.index = index;
        this.realm = realm;
        this.bulkExportFacade = new BulkExportFacade(index);
    }

    @Override
    public Map<String, Object> generate() {
        return Map.of(ESObjectConstants.DSM, Map.of("participant", transformedObject));
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
