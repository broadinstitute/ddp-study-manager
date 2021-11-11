package org.broadinstitute.dsm.model.elastic.migrationscript;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.broadinstitute.dsm.TestHelper;
import org.broadinstitute.dsm.db.MedicalRecord;
import org.broadinstitute.dsm.model.elastic.Util;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class MedicalRecordMigrateTest {

    @BeforeClass
    public static void setUp() {
        TestHelper.setupDB();
    }

    @Test
    public void transformMedicalRecordToMap() {
        List<Object> medicalRecords = Arrays.asList(new MedicalRecord("1", "2", "3", "TYPE"));
        List<Map<String, Object>> listOfMaps = Util.transformObjectCollectionToCollectionMap(medicalRecords);
        Map<String, Object> stringObjectMap = listOfMaps.get(0);
        Assert.assertEquals("1", stringObjectMap.get("medicalRecordId"));
        Assert.assertEquals("2", stringObjectMap.get("institutionId"));
        Assert.assertEquals("3", stringObjectMap.get("ddpInstitutionId"));
        Assert.assertEquals("TYPE", stringObjectMap.get("type"));
    }

    @Test
    public void generateSource() {
        List<Object> medicalRecords = Arrays.asList(new MedicalRecord("1", "2", "3", "TYPE"));
        List<Map<String, Object>> listOfMaps = Util.transformObjectCollectionToCollectionMap(medicalRecords);
        MedicalRecordMigrate migrator = new MedicalRecordMigrate("", "");
        Map<String, Object> resultMap = migrator.generate();
        Map<String, Object> dsm = (Map)resultMap.get("dsm");
        List<Map<String, Object>> medicalRecords1 = (List<Map<String, Object>>) dsm.get("medicalRecords");
        Object medicalRecordsId = medicalRecords1.get(0).get("medicalRecordId");
        assertEquals("1", medicalRecordsId);
    }

    @Test
    public void export() {
        BaseMigrator migrator = new MedicalRecordMigrate("participants_structured.rgp.rgp","rgp");
        migrator.export();
    }

}