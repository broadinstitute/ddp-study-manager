package org.broadinstitute.dsm.model.elastic.export;


import org.broadinstitute.dsm.db.structure.DBElement;
import org.broadinstitute.dsm.model.NameValue;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

public class MappingGeneratorTest {

    @Test
    public void generateTextType() {
        Generator generator = new TestMappingGenerator();
        NameValue nameValue = new NameValue(TestPatchUtil.MEDICAL_RECORD_COLUMN, "value");
        Map<String, Object> objectMap = generator.generate(nameValue);
        Assert.assertEquals(objectMap.keySet().stream().findFirst().get(), BaseGenerator.PROPERTIES);
        String type = extractDeepestLeveleValue(objectMap);
        Assert.assertEquals("text", type);
    }

    @Test
    public void generateBooleanType() {
        Generator generator = new TestMappingGenerator();
        NameValue nameValue = new NameValue(TestPatchUtil.MEDICAL_RECORD_COLUMN, "true");
        Map<String, Object> objectMap = generator.generate(nameValue);
        Assert.assertEquals(objectMap.keySet().stream().findFirst().get(), BaseGenerator.PROPERTIES);
        String type = extractDeepestLeveleValue(objectMap);
        Assert.assertEquals("boolean", type);
    }

    @Test
    public void generateIntegerType() {
        Generator generator = new TestMappingGenerator();
        NameValue nameValue = new NameValue(TestPatchUtil.MEDICAL_RECORD_COLUMN, "45");
        Map<String, Object> objectMap = generator.generate(nameValue);
        Assert.assertEquals(objectMap.keySet().stream().findFirst().get(), BaseGenerator.PROPERTIES);
        String type = extractDeepestLeveleValue(objectMap);
        Assert.assertEquals("integer", type);
    }

    @Test
    public void generateDateType() {
        Generator generator = new TestMappingGenerator();
        NameValue nameValue = new NameValue(TestPatchUtil.MEDICAL_RECORD_COLUMN, "2021-10-30");
        Map<String, Object> objectMap = generator.generate(nameValue);
        Assert.assertEquals(objectMap.keySet().stream().findFirst().get(), BaseGenerator.PROPERTIES);
        String type = extractDeepestLeveleValue(objectMap);
        Assert.assertEquals("date", type);
    }

    private String extractDeepestLeveleValue(Map<String, Object> objectMap) {
        return (String)
                ((Map)
                ((Map)
                ((Map)
                ((Map)
                ((Map)
                ((Map) objectMap
                        .get(BaseGenerator.PROPERTIES))
                        .get(BaseGenerator.DSM_OBJECT))
                        .get(BaseGenerator.PROPERTIES))
                        .get("medicalRecords"))
                        .get(BaseGenerator.PROPERTIES))
                        .get("medical_record_column"))
                        .get("type");
    }

    private static class TestMappingGenerator extends MappingGenerator {

        @Override
        protected DBElement getDBElement() {
            return TestPatchUtil.getColumnNameMap().get(nameValue.getName());
        }
    }
}