package org.broadinstitute.dsm.model.elastic.migration;

import org.broadinstitute.dsm.TestHelper;
import org.junit.BeforeClass;
import org.junit.Test;

public class OncHistoryDetailsMigratorTest {

    @BeforeClass
    public static void setUp() {
        TestHelper.setupDB();
    }

    @Test
    public void export() {
        OncHistoryDetailsMigrator oncHistoryDetailsMigrator = new OncHistoryDetailsMigrator("participants_structured.cmi.angio", "angio");
        oncHistoryDetailsMigrator.export();
    }
}