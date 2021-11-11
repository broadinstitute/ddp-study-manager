package org.broadinstitute.dsm.model.elastic.migrationscript;

import org.broadinstitute.dsm.TestHelper;
import org.junit.BeforeClass;
import org.junit.Test;

public class ParticipantDataMigrateTest {

    @BeforeClass
    public static void setUp() {
        TestHelper.setupDB();
    }

    @Test
    public void export() {
        BaseMigrator participantMigrator = new ParticipantDataMigrate("participants_structured.rgp.rgp", "rgp");
        participantMigrator.export();
    }
}