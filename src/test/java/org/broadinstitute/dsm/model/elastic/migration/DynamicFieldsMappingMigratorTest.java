package org.broadinstitute.dsm.model.elastic.migration;

import junit.framework.TestCase;
import org.broadinstitute.dsm.TestHelper;
import org.broadinstitute.dsm.model.elastic.export.Exportable;
import org.junit.BeforeClass;
import org.junit.Test;

public class DynamicFieldsMappingMigratorTest {

    @BeforeClass
    public static void setUp() {
        TestHelper.setupDB();
    }

    @Test
    public void testExport() {
        Exportable exportable = new DynamicFieldsMappingMigrator("participants_structured.cmi.angio", "angio");
        exportable.export();
    }
}