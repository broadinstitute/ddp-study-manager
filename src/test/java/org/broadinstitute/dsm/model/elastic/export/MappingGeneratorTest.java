package org.broadinstitute.dsm.model.elastic.export;


import org.broadinstitute.dsm.db.structure.DBElement;
import org.broadinstitute.dsm.model.NameValue;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Map;

public class MappingGeneratorTest {

    static Generator generator;

    @BeforeClass
    public static void setUp() {
        generator = new TestMappingGenerator(new TypeParser());
    }

    @Test
    public void generateTextType() {
        NameValue nameValue = new NameValue(TestPatchUtil.MEDICAL_RECORD_COLUMN, "value");
        Map<String, Object> objectMap = generator.generate(nameValue);
        Assert.assertEquals(objectMap.keySet().stream().findFirst().get(), BaseGenerator.PROPERTIES);
        String type = extractDeepestLeveleValue(objectMap);
        Assert.assertEquals("text", type);
    }

    @Test
    public void generateBooleanType() {
        NameValue nameValue = new NameValue(TestPatchUtil.MEDICAL_RECORD_COLUMN, "true");
        Map<String, Object> objectMap = generator.generate(nameValue);
        Assert.assertEquals(objectMap.keySet().stream().findFirst().get(), BaseGenerator.PROPERTIES);
        String type = extractDeepestLeveleValue(objectMap);
        Assert.assertEquals("boolean", type);
    }

    @Test
    public void generateIntegerType() {
        NameValue nameValue = new NameValue(TestPatchUtil.MEDICAL_RECORD_COLUMN, "45");
        Map<String, Object> objectMap = generator.generate(nameValue);
        Assert.assertEquals(objectMap.keySet().stream().findFirst().get(), BaseGenerator.PROPERTIES);
        String type = extractDeepestLeveleValue(objectMap);
        Assert.assertEquals("integer", type);
    }

    @Test
    public void generateDateType() {
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

        public TestMappingGenerator(Parser typeParser) {
            super(typeParser);
        }

        @Override
        protected DBElement getDBElement() {
            return TestPatchUtil.getColumnNameMap().get(nameValue.getName());
        }
    }
}