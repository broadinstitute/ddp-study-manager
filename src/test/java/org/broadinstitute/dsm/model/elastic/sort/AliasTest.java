package org.broadinstitute.dsm.model.elastic.sort;

import static org.junit.Assert.*;

import org.junit.Test;

public class AliasTest {

    @Test
    public void valueOf() {
        assertEquals(Alias.M, Alias.of("m"));
        assertEquals(Alias.ACTIVITIES, Alias.of("unknown"));
    }

}