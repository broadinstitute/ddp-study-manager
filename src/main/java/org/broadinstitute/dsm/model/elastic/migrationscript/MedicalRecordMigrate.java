package org.broadinstitute.dsm.model.elastic.migrationscript;

import java.util.*;

import org.broadinstitute.dsm.db.MedicalRecord;
import org.broadinstitute.dsm.statics.ESObjectConstants;

public class MedicalRecordMigrate extends BaseCollectionMigrator {

    public MedicalRecordMigrate(String index, String realm) {
        super(index, realm, ESObjectConstants.MEDICAL_RECORDS, "medicalRecordId");
    }

    @Override
    protected Map<String, Object> getDataByRealm() {
        return (Map) MedicalRecord.getMedicalRecords(realm);
    }

}
