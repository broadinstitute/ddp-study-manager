package org.broadinstitute.dsm.model.elastic.migrationscript;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.broadinstitute.dsm.TestHelper;
import org.broadinstitute.dsm.db.MedicalRecord;
import org.broadinstitute.dsm.model.elastic.Util;
import org.broadinstitute.dsm.statics.DBConstants;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class MedicalRecordMigrateTest {


    @BeforeClass
    public static void setUp() {
        TestHelper.setupDB();
    }

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

    @Test
    public void medicalRecordMappingMerged() {
        Map<String, Object> medicalRecordMappingMerged =
                MedicalRecordMigrate.medicalRecordMappingMerged;
        assertEquals(37, medicalRecordMappingMerged.size());
    }

    @Test
    public void transformMedicalRecordToMap() {
        List<Object> medicalRecords = Arrays.asList(new MedicalRecord("1", "2", "3", "TYPE"));
        List<Map<String, Object>> listOfMaps = Util.transformObjectCollectionToCollectionMap(medicalRecords, MedicalRecord.class);
        Map<String, Object> stringObjectMap = listOfMaps.get(0);
        Assert.assertEquals("1", stringObjectMap.get("medicalRecordId"));
        Assert.assertEquals("2", stringObjectMap.get("institutionId"));
        Assert.assertEquals("3", stringObjectMap.get("ddpInstitutionId"));
        Assert.assertEquals("TYPE", stringObjectMap.get("type"));
    }

    @Test
    public void generateSource() {
        List<Object> medicalRecords = Arrays.asList(new MedicalRecord("1", "2", "3", "TYPE"));
        List<Map<String, Object>> listOfMaps = Util.transformObjectCollectionToCollectionMap(medicalRecords, MedicalRecord.class);
        Map<String, Object> resultMap = BaseMigrator.generateSource(listOfMaps);
        Map<String, Object> dsm = (Map)resultMap.get("dsm");
        List<Map<String, Object>> medicalRecords1 = (List<Map<String, Object>>) dsm.get("medicalRecords");
        Object medicalRecordsId = medicalRecords1.get(0).get("medicalRecordId");
        assertEquals("1", medicalRecordsId);
    }

    @Test
    public void exportMedicalRecordsToES() {
        MedicalRecordMigrate.exportMedicalRecordsToES();
    }

}