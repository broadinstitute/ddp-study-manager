package org.broadinstitute.dsm.model.elastic.export;


import org.broadinstitute.dsm.db.structure.DBElement;
import org.broadinstitute.dsm.model.NameValue;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

public class MappingGeneratorTest {

    @Test
    public void generate() {
        Generator generator = new TestMappingGenerator();
        NameValue nameValue = new NameValue(TestPatchUtil.MEDICAL_RECORD_COLUMN, "value");
        Map<String, Object> objectMap = generator.generate(nameValue);
        Assert.assertEquals(objectMap.keySet().stream().findFirst().get(), BaseGenerator.PROPERTIES);
        ((Map) ((Map)((Map) ((Map)((Map) objectMap.get(BaseGenerator.PROPERTIES)).get(BaseGenerator.DSM_OBJECT)).get(BaseGenerator.PROPERTIES)).get("medicalRecords")).get(BaseGenerator.PROPERTIES)).get();
        String value = (String)((Map)((Map)objectMap.get(SourceGenerator.DSM_OBJECT)).get("medicalRecords")).get(TestPatchUtil.MEDICAL_RECORD_COLUMN);
        Assert.assertEquals("value", value);


    }

    private static class TestMappingGenerator extends MappingGenerator {

        @Override
        protected DBElement getDBElement() {
            return TestPatchUtil.getColumnNameMap().get(nameValue.getName());
        }
    }
}