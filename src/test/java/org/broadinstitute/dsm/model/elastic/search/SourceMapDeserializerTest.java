package org.broadinstitute.dsm.model.elastic.search;

import static org.junit.Assert.*;

import java.util.Map;

import org.broadinstitute.dsm.statics.ESObjectConstants;
import org.junit.Test;

public class SourceMapDeserializerTest {

    @Test
    public void convertFollowUpsJsonToList() {
        Map.of(
                "REGISTRATION_TYPE", "Self",
                "REGISTRATION_STATUS", "Registered"
        );
        Map<String, Object> outerProperties = Map.of(
                ESObjectConstants.DYNAMIC_FIELDS,
        )
    }
}