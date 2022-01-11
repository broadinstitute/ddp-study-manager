package org.broadinstitute.dsm.model.elastic.export.generate;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.broadinstitute.dsm.db.structure.DBElement;
import org.broadinstitute.dsm.model.NameValue;
import org.broadinstitute.dsm.model.elastic.Util;
import org.broadinstitute.dsm.model.elastic.export.parse.Parser;
import org.broadinstitute.dsm.model.elastic.export.TestPatchUtil;
import org.broadinstitute.dsm.model.elastic.export.parse.ValueParser;
import org.junit.Test;
import org.junit.Assert;

public class SourceGeneratorTest {

    @Test
    public void generateCollection() {
        NameValue nameValue = new NameValue(TestPatchUtil.MEDICAL_RECORD_COLUMN, "value");
        GeneratorPayload generatorPayload = new GeneratorPayload(nameValue, 0);
        Generator generator = new TestSourceGenerator(new ValueParser(), generatorPayload);
        Map<String, Object> objectMap = generator.generate();
        Assert.assertEquals(objectMap.keySet().stream().findFirst().get(), SourceGenerator.DSM_OBJECT);
        List<Map<String, Object>> medicalRecords = (List) ((Map) objectMap.get(SourceGenerator.DSM_OBJECT)).get("medicalRecords");
        Optional<Map<String, Object>> first = medicalRecords.stream()
                .filter(i -> i.get(Util.underscoresToCamelCase(TestPatchUtil.MEDICAL_RECORD_COLUMN)) != null)
                .findFirst();
        first.ifPresentOrElse(val -> Assert.assertEquals("value", val.get(Util.underscoresToCamelCase(TestPatchUtil.MEDICAL_RECORD_COLUMN))), Assert::fail);
    }

    @Test
    public void generateNumeric() {
        NameValue nameValue = new NameValue(TestPatchUtil.MEDICAL_RECORD_COLUMN, "1");
        GeneratorPayload generatorPayload = new GeneratorPayload(nameValue, 0);
        Generator generator = new TestSourceGenerator(new ValueParser(), generatorPayload);
        Map<String, Object> objectMap = generator.generate();
        Assert.assertEquals(objectMap.keySet().stream().findFirst().get(), SourceGenerator.DSM_OBJECT);
        List<Map<String, Object>> medicalRecords = (List) ((Map) objectMap.get(SourceGenerator.DSM_OBJECT)).get("medicalRecords");
        Optional<Map<String, Object>> first = medicalRecords.stream()
                .filter(i -> i.get(Util.underscoresToCamelCase(TestPatchUtil.MEDICAL_RECORD_COLUMN)) != null)
                .findFirst();
        first.ifPresentOrElse(val -> Assert.assertEquals(1L, val.get(Util.underscoresToCamelCase(TestPatchUtil.MEDICAL_RECORD_COLUMN))), Assert::fail);
    }

    @Test
    public void generateFromJson() {
        NameValue nameValue = new NameValue(TestPatchUtil.TISSUE_RECORD_COLUMN, "{\"DDP_INSTANCE\": \"TEST\", \"DPP_VALUE\": \"VALUE\"}");
        GeneratorPayload generatorPayload = new GeneratorPayload(nameValue, 0);
        Generator generator = new TestSourceGenerator(new ValueParser(), generatorPayload);
        Map<String, Object> objectMap = generator.generate();
        Assert.assertEquals(objectMap.keySet().stream().findFirst().get(), SourceGenerator.DSM_OBJECT);

        List<Map<String, Object>> tissueRecords = (List<Map<String, Object>>) ((Map<String, Object>) objectMap
                .get(SourceGenerator.DSM_OBJECT))
                .get("tissueRecords");
        Optional<Map<String, Object>> maybeDdpInstance = tissueRecords.stream()
                .filter(i -> i.get(Util.underscoresToCamelCase("DDP_INSTANCE")) != null)
                .findFirst();
        maybeDdpInstance.ifPresentOrElse(m -> Assert.assertEquals("TEST", m.get(Util.underscoresToCamelCase("DDP_INSTANCE"))), Assert::fail);
    }

    @Test
    public void generateFromMultipleNameValues() {
        final String externalOrderNumber = "externalOrderNumber";
        final String bspCollaboratorSampleId = "bspCollaboratorSampleId";
        List<NameValue> nameValues = Arrays.asList(new NameValue(externalOrderNumber, 12), new NameValue(bspCollaboratorSampleId, 55));
        TestGeneratorPayload testGeneratorPayload = new TestGeneratorPayload(nameValues, 1);
        SourceGenerator sourceGenerator = new SourceGenerator(new ValueParser(), testGeneratorPayload);
        Map<String, Object> generatedMap = sourceGenerator.generate();
        // dsm.kitRequestShipping.[{}]
        List<Map<String, Object>> kitRequestShippings = (List<Map<String, Object>>) ((Map) generatedMap.get("dsm")).get("kitRequestShipping");
        for (Map<String, Object> kitRequestShipping : kitRequestShippings) {
            Object externalOrderNumberVal = kitRequestShipping.get(externalOrderNumber);
            Object collaboratorSampleIdVal = kitRequestShipping.get(bspCollaboratorSampleId);
            Assert.assertEquals("12", externalOrderNumberVal);
            Assert.assertEquals("55", collaboratorSampleIdVal);
        }
    }

    private static class TestSourceGenerator extends CollectionSourceGenerator {

        public TestSourceGenerator(Parser parser, GeneratorPayload generatorPayload) {
            super(parser,generatorPayload);
        }

        @Override
        protected DBElement getDBElement() {
            return TestPatchUtil.getColumnNameMap().get(getNameValue().getName());
        }
    }

    private class TestGeneratorPayload extends GeneratorPayload {

        List<NameValue> nameValue;

        public TestGeneratorPayload(List<NameValue> nameValues, int id) {
            this.nameValue = nameValues;
            this.recordId = id;
        }


        public List<NameValue> getNameValue() {
            return nameValue;
        }
    }
}

