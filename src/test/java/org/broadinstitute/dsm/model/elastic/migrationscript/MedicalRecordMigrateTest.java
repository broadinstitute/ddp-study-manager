package org.broadinstitute.dsm.model.elastic.migrationscript;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

import org.broadinstitute.dsm.model.elastic.Util;
import org.broadinstitute.dsm.statics.DBConstants;
import org.junit.Test;

public class MedicalRecordMigrateTest {

    @Test
    public void collectMedicalRecordColumns() {
        MedicalRecordMigrate medicalRecordMigrate = new MedicalRecordMigrate();
        List<String> columnNames = medicalRecordMigrate.collectMedicalRecordColumns();
        assertTrue(columnNames.contains(DBConstants.MR_RECEIVED));
    }

    @Test
    public void swapToCamelCases() {
        MedicalRecordMigrate medicalRecordMigrate = new MedicalRecordMigrate();
        List<String> columnNames = medicalRecordMigrate.collectMedicalRecordColumns();
        List<String> camelCaseColumns = medicalRecordMigrate.swapToCamelCases(columnNames);
        assertEquals("mrReceived", camelCaseColumns.stream().filter("mrReceived"::equals).findFirst().get());
    }
}