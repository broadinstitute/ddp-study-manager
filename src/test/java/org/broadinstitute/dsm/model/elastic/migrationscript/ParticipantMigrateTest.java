package org.broadinstitute.dsm.model.elastic.migrationscript;

import static org.junit.Assert.*;

import org.broadinstitute.dsm.TestHelper;
import org.junit.Before;
import org.junit.Test;

public class ParticipantMigrateTest {

    @Before
    public void setUp() {
        TestHelper.setupDB();
    }

    @Test
    public void export() {
        ParticipantMigrate participantMigrate = new ParticipantMigrate("participants_structured.cmi.cmi-brain", "brain");
        participantMigrate.export();
    }
}