package org.broadinstitute.dsm.model.elastic.export;

import com.google.gson.Gson;
import org.broadinstitute.dsm.model.NameValue;
import org.broadinstitute.dsm.model.elastic.ESDsm;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.Map;

public class CollectionProcessorTest {

    @Test
    public void testProcess() {

        String json = "{\"medicalRecords\":[{\"id\":10000000000000000,\"mr\":\"mr old\"}]}";
        ESDsm esDsm = new Gson().fromJson(json, ESDsm.class);
        String propertyName = "medicalRecords";
        NameValue nameValue = new NameValue("mr", "mr updated");
        long recordId = 5;
        GeneratorPayload generatorPayload = new GeneratorPayload(nameValue, recordId);

        CollectionProcessor collectionProcessor = new CollectionProcessor(esDsm, propertyName, generatorPayload);

        List<Map<String, Object>> process = collectionProcessor.process();

        Map<String, Object> updatedObject = process.get(0);
        Map<String, Object> oldObject = new Gson().fromJson(json, Map.class);

        Assert.assertEquals(oldObject, updatedObject);

    }

    private static class MedicalRecord {


    }
}

