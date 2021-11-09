package org.broadinstitute.dsm.model.elastic.migrationscript;

import java.util.*;

import org.broadinstitute.dsm.db.MedicalRecord;
import org.broadinstitute.dsm.statics.ESObjectConstants;

public class MedicalRecordMigrate extends BaseMigrator {

    public MedicalRecordMigrate(String index, String realm) {
        super(index, realm, ESObjectConstants.MEDICAL_RECORDS);
    }

    @Override
    protected Map<String, List<Object>> getDataByRealm() {
        return (Map) MedicalRecord.getMedicalRecords(realm);
    }

}
