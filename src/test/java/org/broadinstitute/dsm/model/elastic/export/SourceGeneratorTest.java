package org.broadinstitute.dsm.model.elastic.export;

import java.util.Map;

import org.broadinstitute.dsm.db.structure.DBElement;
import org.broadinstitute.dsm.model.NameValue;
import org.broadinstitute.dsm.util.PatchUtil;
import org.junit.Test;
import org.junit.Assert;

public class SourceGeneratorTest {

    @Test
    public void generate() {
        Generator generator = new TestSourceGenerator();
        NameValue nameValue = new NameValue("medical_record_column", "value");
        Map<String, Object> objectMap = generator.generate(nameValue);
        Assert.assertEquals(objectMap.keySet().stream().findFirst().get(), SourceGenerator.DSM_OBJECT);
        String value = (String)((Map)((Map)objectMap.get(SourceGenerator.DSM_OBJECT)).get("medicalRecords")).get("medical_record_column");
        Assert.assertEquals("value", value);
    }

    private static class TestSourceGenerator extends SourceGenerator {

        @Override
        protected DBElement getDBElement(NameValue nameValue) {
            return TestPatchUtil.getColumnNameMap().get(nameValue.getName());
        }
    }

    private static class TestPatchUtil extends PatchUtil {

        public static Map<String, DBElement> getColumnNameMap() {
            DBElement dbElement = new DBElement("ddp_medical_record", "m", "pr", "medical_record_column");
            return Map.of("medical_record_column", dbElement);
        }

    }
}

