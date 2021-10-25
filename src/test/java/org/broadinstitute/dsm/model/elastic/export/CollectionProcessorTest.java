package org.broadinstitute.dsm.model.elastic.export;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.broadinstitute.dsm.model.NameValue;
import org.broadinstitute.dsm.model.elastic.ESDsm;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class CollectionProcessorTest {

    @Test
    public void testProcess() throws IOException {
        String propertyName = "medicalRecords";
        int recordId = 5;
        String oldValue = "mr_old";
        String json = String.format("{\"%s\":[{\"id\":%s,\"mr\":\"%s\"}]}", propertyName, recordId, oldValue);

        ObjectMapper objectMapper = new ObjectMapper();

        ESDsm esDsm = objectMapper.readValue(json, ESDsm.class);

        NameValue nameValue = new NameValue("mr", "mr_updated");
        GeneratorPayload generatorPayload = new GeneratorPayload(nameValue, recordId);

        CollectionProcessor collectionProcessor = new CollectionProcessor(esDsm, propertyName, generatorPayload);

        List<Map<String, Object>> updatedList = collectionProcessor.process();

        Map<String, Object> updatedObject = updatedList.get(0);
        Map<String, Object> oldObject = ((Map) ((List) objectMapper.readValue(json, Map.class).get(propertyName)).get(0));

        Assert.assertNotEquals(oldObject, updatedObject);

    }
}

