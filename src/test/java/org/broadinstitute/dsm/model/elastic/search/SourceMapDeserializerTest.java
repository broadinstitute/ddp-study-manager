package org.broadinstitute.dsm.model.elastic.search;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Map;

import org.broadinstitute.dsm.statics.ESObjectConstants;
import org.broadinstitute.dsm.util.ObjectMapperSingleton;
import org.junit.Assert;
import org.junit.Test;

public class SourceMapDeserializerTest {

    @Test
    public void convertFollowUpsJsonToList() {
        Map<String, String> dynamicFields = Map.of(
                "registrationType", "Self",
                "registrationStatus", "Registered"
        );
        Map<String, Object> outerProperties = Map.of(ESObjectConstants.DYNAMIC_FIELDS, dynamicFields);
        SourceMapDeserializer sourceMapDeserializer = new SourceMapDeserializer();
        sourceMapDeserializer.outerProperty = ESObjectConstants.PARTICIPANT_DATA;
        try {
            Map<String, Object> dynamicFieldsValueAsJson = ObjectMapperSingleton.instance().readValue(sourceMapDeserializer.getDynamicFieldsValueAsJson(outerProperties), Map.class);
            Assert.assertEquals(dynamicFieldsValueAsJson.get("REGISTRATION_TYPE"), "Self");
            Assert.assertEquals(dynamicFieldsValueAsJson.get("REGISTRATION_STATUS"), "Registered");
        } catch (IOException e) {
            Assert.fail();
        }
    }
}