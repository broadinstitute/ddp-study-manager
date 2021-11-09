package org.broadinstitute.dsm.model.elastic.migrationscript;

import static org.junit.Assert.*;

import org.broadinstitute.dsm.TestHelper;
import org.junit.Before;
import org.junit.Test;

public class OncHistoryDetailsMigrateTest {

    @Before
    public void setUp() throws Exception {
        TestHelper.setupDB();
    }

    @Test
    public void export() {
        OncHistoryDetailsMigrate oncHistoryDetailsMigrate = new OncHistoryDetailsMigrate("participants_structured.cmi.cmi-brain","brain");
        oncHistoryDetailsMigrate.export();
    }
}