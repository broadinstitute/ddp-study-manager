package org.broadinstitute.dsm.model.elastic.export;

import java.util.Map;

import org.broadinstitute.dsm.db.structure.DBElement;
import org.broadinstitute.dsm.model.NameValue;
import org.broadinstitute.dsm.util.PatchUtil;
import org.junit.Test;
import org.junit.Assert;

public class SourceValueGeneratorTest {

    public static final String MEDICAL_RECORD_COLUMN = "medical_record_column";

    @Test
    public void generate() {
        ValueGenerator generator = new TestSourceGenerator();
        NameValue nameValue = new NameValue(MEDICAL_RECORD_COLUMN, "value");
        Map<String, Object> objectMap = generator.generate(nameValue);
        Assert.assertEquals(objectMap.keySet().stream().findFirst().get(), SourceGenerator.DSM_OBJECT);
        String value = (String)((Map)((Map)objectMap.get(SourceGenerator.DSM_OBJECT)).get("medicalRecords")).get(MEDICAL_RECORD_COLUMN);
        Assert.assertEquals("value", value);
    }

    private static class TestSourceGenerator extends SourceGenerator {

        @Override
        protected DBElement getDBElement() {
            return TestPatchUtil.getColumnNameMap().get(nameValue.getName());
        }
    }

    private static class TestPatchUtil extends PatchUtil {

        public static Map<String, DBElement> getColumnNameMap() {
            DBElement dbElement = new DBElement("ddp_medical_record", "m", "pr", MEDICAL_RECORD_COLUMN);
            return Map.of(MEDICAL_RECORD_COLUMN, dbElement);
        }

    }
}

