package org.broadinstitute.dsm.model.elastic.export;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.broadinstitute.dsm.db.structure.DBElement;
import org.broadinstitute.dsm.model.NameValue;
import org.broadinstitute.dsm.model.elastic.ESDsm;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class CollectionProcessorTest {

    @Test
    public void testProcess() throws IOException {
        String propertyName = "medicalRecords";
        double recordId = 5;
        String oldValue = "mr_old";
        String json = String.format("{\"%s\":[{\"id\":%s,\"mr\":\"%s\"}]}", propertyName, recordId, oldValue);

        ObjectMapper objectMapper = new ObjectMapper();

        ESDsm esDsm = objectMapper.readValue(json, ESDsm.class);

        NameValue nameValue = new NameValue("mr", "mr_updated");
        GeneratorPayload generatorPayload = new GeneratorPayload(nameValue, (int)recordId);

        ValueParser valueParser = new ValueParser();
        CollectionProcessor collectionProcessor = new CollectionProcessor(esDsm, propertyName, generatorPayload, valueParser);

        List<Map<String, Object>> updatedList = collectionProcessor.process();

        Map<String, Object> updatedObject = updatedList.get(0);
        Map<String, Object> oldObject = ((Map) ((List) objectMapper.readValue(json, Map.class).get(propertyName)).get(0));

        Assert.assertNotEquals(oldObject, updatedObject);

    }

    @Test
    public void updateIfExistsOrPut() throws IOException {
        String propertyName = "medicalRecords";
        double recordId = 5;
        String json = String.format("{\"%s\":[{\"id\":%s,\"mr\":\"%s\"}]}", propertyName, recordId, "value");;

        ObjectMapper objectMapper = new ObjectMapper();

        ESDsm esDsm = objectMapper.readValue(json, ESDsm.class);

        NameValue nameValue = new NameValue(TestPatchUtil.MEDICAL_RECORD_COLUMN, "val");
        GeneratorPayload generatorPayload = new GeneratorPayload(nameValue, 10);

        CollectionProcessor collectionProcessor = new TestCollectionProcessor(esDsm, propertyName, new ValueParser(), generatorPayload);

        List<Map<String, Object>> updatedList = collectionProcessor.process();

        Assert.assertEquals(2, updatedList.size());

    }

    @Test
    public void updateIfExists() throws IOException {
        String propertyName = "medicalRecords";
        double recordId = 5;
        String json = String.format("{\"%s\":[{\"id\":%s,\"TEST1\":\"%s\", \"TEST2\":\"TEST_VAL2\"}]}", propertyName, recordId, "value");;

        ObjectMapper objectMapper = new ObjectMapper();

        ESDsm esDsm = objectMapper.readValue(json, ESDsm.class);

        NameValue nameValue = new NameValue(TestPatchUtil.MEDICAL_RECORD_COLUMN, "{\"TEST1\":\"TEST_VAL\", \"TEST2\":\"TEST_VAL3\"}");
        GeneratorPayload generatorPayload = new GeneratorPayload(nameValue, 5);

        CollectionProcessor collectionProcessor = new TestCollectionProcessor(esDsm, propertyName, new ValueParser(), generatorPayload);

        List<Map<String, Object>> updatedList = collectionProcessor.process();

        updatedList.stream()
                .filter(i -> i.containsKey("TEST1"))
                .findFirst()
                .ifPresentOrElse(m -> m.get("TEST1").equals("TEST_VAL"), Assert::fail);

        updatedList.stream()
                .filter(i -> i.containsKey("TEST2"))
                .findFirst()
                .ifPresentOrElse(m -> m.get("TEST2").equals("TEST_VAL3"), Assert::fail);
    }

    private static class TestCollectionProcessor extends CollectionProcessor {

        public TestCollectionProcessor(ESDsm esDsm,String propertyName, Parser parser, GeneratorPayload generatorPayload) {
            super(esDsm, propertyName, generatorPayload, parser);
        }

        @Override
        protected DBElement getDBElement() {
            return TestPatchUtil.getColumnNameMap().get(getNameValue().getName());
        }
    }

}

