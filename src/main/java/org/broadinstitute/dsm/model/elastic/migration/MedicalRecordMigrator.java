package org.broadinstitute.dsm.model.elastic.migration;

import org.broadinstitute.dsm.db.MedicalRecord;
import org.broadinstitute.dsm.model.elastic.export.generate.GeneratorPayload;
import org.broadinstitute.dsm.model.elastic.export.parse.Parser;
import org.broadinstitute.dsm.statics.ESObjectConstants;

import java.util.Map;

public class MedicalRecordMigrator extends BaseCollectionMigrator {

    public MedicalRecordMigrator(String index, String realm) {
        super(index, realm, "medicalRecord");
    }

    @Override
    protected Map<String, Object> getDataByRealm() {
        return (Map) MedicalRecord.getMedicalRecords(realm);
    }

    @Override
    public void setParser(Parser parser) {

    }

    @Override
    public void setPayload(GeneratorPayload generatorPayload) {

    }
}
