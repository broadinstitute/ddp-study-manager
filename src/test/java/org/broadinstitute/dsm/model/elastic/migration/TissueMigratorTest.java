package org.broadinstitute.dsm.model.elastic.migration;

import static org.junit.Assert.*;

import java.util.Map;

import org.broadinstitute.dsm.TestHelper;
import org.junit.BeforeClass;
import org.junit.Test;

public class TissueMigratorTest {

    @BeforeClass
    public static void setUp() {
        TestHelper.setupDB();
    }

    @Test
    public void getDataByRealm() {
        TissueMigrator tissueMigrator = new TissueMigrator("participants_structured.cmi.angio", "angio", "tissues");
        tissueMigrator.export();
    }
}