package org.broadinstitute.dsm.model.elastic.search;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.*;

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

        Map<String, Object> outerProperties = Map.of(
                "ddpInstanceId", 12,
                ESObjectConstants.DYNAMIC_FIELDS, dynamicFields
        );


        SourceMapDeserializer sourceMapDeserializer = new SourceMapDeserializer();
        sourceMapDeserializer.outerProperty = ESObjectConstants.PARTICIPANT_DATA;
        try {
            Map<String, Object> dynamicFieldsValueAsJson = ObjectMapperSingleton.instance().readValue(sourceMapDeserializer.getDynamicFieldsValueAsJson(outerProperties), Map.class);
            Assert.assertEquals(dynamicFieldsValueAsJson.get("REGISTRATION_TYPE"), "Self");
            Assert.assertEquals(dynamicFieldsValueAsJson.get("REGISTRATION_STATUS"), "Registered");
            Assert.assertEquals(12, outerProperties.get("ddpInstanceId"));
        } catch (IOException e) {
            Assert.fail();
        }
    }

    @Test
    public void convertSeveralDynamicFields() {

        Map<String, String> dynamicFields1 = new HashMap<>(Map.of(
                "registrationType", "Self",
                "registrationStatus", "Registered"
        ));

        Map<String, String> dynamicFields2 = new HashMap<>(Map.of(
                "registrationType", "Self",
                "registrationStatus", "Registered"
        ));

        Map<String, Object> outerProperties1 = new HashMap<>(Map.of(
                "ddpInstanceId", 12,
                ESObjectConstants.DYNAMIC_FIELDS, dynamicFields1
        ));

        Map<String, Object> outerProperties2 = new HashMap<>(Map.of(
                "ddpInstanceId", 13,
                ESObjectConstants.DYNAMIC_FIELDS, dynamicFields2
        ));

        Map<String, Object> participantData = new HashMap<>(Map.of("participantData", new ArrayList<>(Arrays.asList(outerProperties1, outerProperties2))));
        Map<String, Object> dsm = new HashMap<>(Map.of("dsm", participantData));

        Optional<ElasticSearchParticipantDto> des = new SourceMapDeserializer().deserialize(dsm);

        System.out.println(des);


    }
}