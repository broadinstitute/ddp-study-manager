package org.broadinstitute.dsm.model.elastic.migration;

import static org.junit.Assert.*;

import org.broadinstitute.dsm.TestHelper;
import org.junit.BeforeClass;
import org.junit.Test;

public class OncHistoryMigratorTest {

    @BeforeClass
    public static void setUp() {
        TestHelper.setupDB();
    }

    @Test
    public void export() {
        OncHistoryMigrator oncHistoryMigrator = new OncHistoryMigrator("participants_structured.cmi.angio", "angio");
        oncHistoryMigrator.export();
    }

}