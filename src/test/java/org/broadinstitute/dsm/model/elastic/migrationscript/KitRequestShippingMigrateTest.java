package org.broadinstitute.dsm.model.elastic.migrationscript;

import org.broadinstitute.dsm.TestHelper;
import org.junit.Before;
import org.junit.Test;

public class KitRequestShippingMigrateTest {

    @Before
    public void setUp() {
        TestHelper.setupDB();
    }

    @Test
    public void export() {
        KitRequestShippingMigrate kitRequestShippingMigrate = new KitRequestShippingMigrate("participants_structured.cmi.cmi-brain", "brain");
        kitRequestShippingMigrate.export();
    }

}