package org.broadinstitute.dsm.model.elastic.export;

import java.util.Map;

import org.broadinstitute.dsm.db.structure.DBElement;
import org.broadinstitute.dsm.model.NameValue;
import org.junit.Test;
import org.junit.Assert;

public class SourceGeneratorTest {

    @Test
    public void generate() {
        Generator generator = new TestSourceGenerator(new ValueParser());
        NameValue nameValue = new NameValue(TestPatchUtil.MEDICAL_RECORD_COLUMN, "value");
        Map<String, Object> objectMap = generator.generate(nameValue);
        Assert.assertEquals(objectMap.keySet().stream().findFirst().get(), SourceGenerator.DSM_OBJECT);
        String value = (String)((Map)((Map)objectMap.get(SourceGenerator.DSM_OBJECT)).get("medicalRecords")).get(TestPatchUtil.MEDICAL_RECORD_COLUMN);
        Assert.assertEquals("value", value);
    }

    @Test
    public void generateNumeric() {
        Generator generator = new TestSourceGenerator(new ValueParser());
        NameValue nameValue = new NameValue(TestPatchUtil.MEDICAL_RECORD_COLUMN, "1");
        Map<String, Object> objectMap = generator.generate(nameValue);
        Assert.assertEquals(objectMap.keySet().stream().findFirst().get(), SourceGenerator.DSM_OBJECT);
        Object value = ((Map)((Map)objectMap.get(SourceGenerator.DSM_OBJECT)).get("medicalRecords")).get(TestPatchUtil.MEDICAL_RECORD_COLUMN);
        Assert.assertEquals(1L, value);
    }

    private static class TestSourceGenerator extends SourceGenerator {

        public TestSourceGenerator(Parser parser) {
            super(parser);
        }

        @Override
        protected DBElement getDBElement() {
            return TestPatchUtil.getColumnNameMap().get(nameValue.getName());
        }
    }

}

