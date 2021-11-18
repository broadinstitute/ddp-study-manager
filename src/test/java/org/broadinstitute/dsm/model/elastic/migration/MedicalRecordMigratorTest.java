package org.broadinstitute.dsm.model.elastic.migration;

import org.broadinstitute.dsm.TestHelper;
import org.junit.BeforeClass;
import org.junit.Test;

public class MedicalRecordMigratorTest {

    @BeforeClass
    public static void setUp() {
        TestHelper.setupDB();
    }

    @Test
    public void export() {
        MedicalRecordMigrator medicalRecordMigrator = new MedicalRecordMigrator("participants_structured.cmi.angio", "angio");
        medicalRecordMigrator.export();
    }

}