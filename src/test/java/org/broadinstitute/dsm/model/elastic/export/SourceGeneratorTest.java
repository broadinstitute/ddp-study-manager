package org.broadinstitute.dsm.model.elastic.export;

import java.util.Map;

import org.broadinstitute.dsm.db.structure.DBElement;
import org.broadinstitute.dsm.model.NameValue;
import org.junit.Test;
import org.junit.Assert;

public class SourceGeneratorTest {

    @Test
    public void generate() {
        NameValue nameValue = new NameValue(TestPatchUtil.MEDICAL_RECORD_COLUMN, "value");
        GeneratorPayload generatorPayload = new GeneratorPayload(nameValue, 0);
        Generator generator = new TestSourceGenerator(new ValueParser(), generatorPayload);
        Map<String, Object> objectMap = generator.generate();
        Assert.assertEquals(objectMap.keySet().stream().findFirst().get(), SourceGenerator.DSM_OBJECT);
        String value = (String)((Map)((Map)objectMap.get(SourceGenerator.DSM_OBJECT)).get("medicalRecords")).get(TestPatchUtil.MEDICAL_RECORD_COLUMN);
        Assert.assertEquals("value", value);
    }

    @Test
    public void generateNumeric() {
        NameValue nameValue = new NameValue(TestPatchUtil.MEDICAL_RECORD_COLUMN, "1");
        GeneratorPayload generatorPayload = new GeneratorPayload(nameValue, 0);
        Generator generator = new TestSourceGenerator(new ValueParser(), generatorPayload);
        Map<String, Object> objectMap = generator.generate();
        Assert.assertEquals(objectMap.keySet().stream().findFirst().get(), SourceGenerator.DSM_OBJECT);
        Object value = ((Map)((Map)objectMap.get(SourceGenerator.DSM_OBJECT)).get("medicalRecords")).get(TestPatchUtil.MEDICAL_RECORD_COLUMN);
        Assert.assertEquals(1L, value);
    }

    @Test
    public void generateFromJson() {
        NameValue nameValue = new NameValue(TestPatchUtil.MEDICAL_RECORD_COLUMN, "{\"DDP_INSTANCE\": \"TEST\"}");
        GeneratorPayload generatorPayload = new GeneratorPayload(nameValue, 0);
        Generator generator = new TestSourceGenerator(new ValueParser(), generatorPayload);
        Map<String, Object> objectMap = generator.generate();
        Assert.assertEquals(objectMap.keySet().stream().findFirst().get(), SourceGenerator.DSM_OBJECT);
        Object value =
                ((Map)((Map)objectMap
                        .get(SourceGenerator.DSM_OBJECT))
                        .get("medicalRecords"))
                        .get("DDP_INSTANCE");
        Assert.assertEquals("TEST", value);
    }

    private static class TestSourceGenerator extends SourceGenerator {

        public TestSourceGenerator(Parser parser, GeneratorPayload generatorPayload) {
            super(parser,generatorPayload);
        }

        @Override
        protected DBElement getDBElement() {
            return TestPatchUtil.getColumnNameMap().get(getNameValue().getName());
        }
    }

}

