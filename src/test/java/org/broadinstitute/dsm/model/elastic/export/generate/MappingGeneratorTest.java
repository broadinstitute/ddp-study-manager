package org.broadinstitute.dsm.model.elastic.export.generate;


import org.broadinstitute.dsm.db.structure.DBElement;
import org.broadinstitute.dsm.model.NameValue;
import org.broadinstitute.dsm.model.elastic.Util;
import org.broadinstitute.dsm.model.elastic.export.parse.Parser;
import org.broadinstitute.dsm.model.elastic.export.TestPatchUtil;
import org.broadinstitute.dsm.model.elastic.export.parse.TypeParser;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class MappingGeneratorTest {


    @Test
    public void generateTextType() {
        GeneratorPayload generatorPayload = new GeneratorPayload(
            new NameValue(TestPatchUtil.MEDICAL_RECORD_COLUMN, "value"),
            0
        );
        Map<String, Object> objectMap = TestMappingGenerator.of(generatorPayload).generate();
        Assert.assertEquals(objectMap.keySet().stream().findFirst().get(), BaseGenerator.PROPERTIES);
        String type = extractDeepestLeveleValue(objectMap);
        Assert.assertEquals("text", type);
    }

    @Test
    public void generateTextTypeWithFields() {
        GeneratorPayload generatorPayload = new GeneratorPayload(
            new NameValue(TestPatchUtil.MEDICAL_RECORD_COLUMN, "value"),
            0
        );
        Map<String, Object> objectMap = TestMappingGenerator.of(generatorPayload).generate();
        Assert.assertEquals(objectMap.keySet().stream().findFirst().get(), BaseGenerator.PROPERTIES);
        String type = extractKeywordType(objectMap);
        Assert.assertEquals("keyword", type);
    }

    @Test
    public void generateBooleanType() {
        GeneratorPayload generatorPayload = new GeneratorPayload(
                new NameValue(TestPatchUtil.MEDICAL_RECORD_COLUMN, "true"),
                0
        );
        Map<String, Object> objectMap = TestMappingGenerator.of(generatorPayload).generate();
        Assert.assertEquals(objectMap.keySet().stream().findFirst().get(), BaseGenerator.PROPERTIES);
        String type = extractDeepestLeveleValue(objectMap);
        Assert.assertEquals("boolean", type);
    }

    @Test
    public void generateDateType() {
        GeneratorPayload generatorPayload = new GeneratorPayload(
                new NameValue(TestPatchUtil.MEDICAL_RECORD_COLUMN, "2021-10-30"),
                0
        );
        Map<String, Object> objectMap = TestMappingGenerator.of(generatorPayload).generate();
        Assert.assertEquals(objectMap.keySet().stream().findFirst().get(), BaseGenerator.PROPERTIES);
        String type = extractDeepestLeveleValue(objectMap);
        Assert.assertEquals("date", type);
    }

    @Test
    public void generateNestedType() {
        GeneratorPayload generatorPayload = new GeneratorPayload(
                new NameValue(TestPatchUtil.MEDICAL_RECORD_COLUMN, "2021-10-30"),
                100
        );
        Map<String, Object> objectMap = TestMappingGenerator.of(generatorPayload).generate();
        Assert.assertEquals(MappingGenerator.NESTED, getMedicalRecordProperty(objectMap).get(MappingGenerator.TYPE));
    }

    @Test
    public void getFieldWithTypeCollectionTrue() {
        GeneratorPayload generatorPayload = new GeneratorPayload(
                new NameValue(TestPatchUtil.MEDICAL_RECORD_COLUMN, "2021-10-30"),
                100
        );
        TestMappingGenerator generator = TestMappingGenerator.of(generatorPayload);
        generator.getOuterPropertyByAlias().setIsCollection(true);
        Object fieldWithType = generator.getFieldWithElement();
        Assert.assertTrue(((Map<String, Object>) fieldWithType).containsKey(Util.ID));
    }

    @Test
    public void generateMapping() {
        GeneratorPayload generatorPayload = new GeneratorPayload(
                new NameValue(TestPatchUtil.MEDICAL_RECORD_COLUMN, "2021-10-30"),
                100
        );
        MappingGenerator generator = new TestMappingGenerator(new TypeParser(), generatorPayload);
        Map<String, Object> resultMap = generator.generate();
        Map<String, Object> dsmLevelProperty = Map.of(generator.getOuterPropertyByAlias().getPropertyName(), Map.of(
                MappingGenerator.TYPE, MappingGenerator.NESTED,
                MappingGenerator.PROPERTIES, Map.of(TestPatchUtil.MEDICAL_RECORD_COLUMN, Map.of(MappingGenerator.TYPE, "date"),
                        Util.ID, Map.of(MappingGenerator.TYPE, MappingGenerator.TYPE_KEYWORD)
                        )));
        Map<String, Object> dsmLevelProperties = Map.of(MappingGenerator.PROPERTIES, dsmLevelProperty);
        Map<String, Object> dsmLevel = Map.of(MappingGenerator.DSM_OBJECT, dsmLevelProperties);
        Map<String, Object> topLevel = Map.of(MappingGenerator.PROPERTIES, dsmLevel);
        Assert.assertTrue(topLevel.equals(resultMap));
    }

    @Test
    public void merge() {
        NameValue nameValue = new NameValue("d.data", "{\"DDP_INSTANCE\": \"TEST\"}");
        GeneratorPayload generatorPayload = new GeneratorPayload(nameValue, 0);
        TypeParser parser = new TypeParser();
        MappingGenerator mappingGenerator = new MappingGenerator(parser, generatorPayload);
        Map<String, Object> base = new HashMap<>();
        base = mappingGenerator.merge(base, mappingGenerator.generate());
        NameValue nameValue2 = new NameValue("d.data", "{\"DDP_INSTANCE1\": \"TEST1\"}");
        GeneratorPayload generatorPayload2 = new GeneratorPayload(nameValue2, 0);
        mappingGenerator = new MappingGenerator(parser, generatorPayload2);
        base = mappingGenerator.merge(base, mappingGenerator.generate());
        Object value = ((Map) ((Map) ((Map) ((Map) ((Map) base
                .get(MappingGenerator.PROPERTIES))
                .get(MappingGenerator.DSM_OBJECT))
                .get(MappingGenerator.PROPERTIES))
                .get(mappingGenerator.getOuterPropertyByAlias().getPropertyName()))
                .get(MappingGenerator.PROPERTIES))
                .get("DDP_INSTANCE1");
        Assert.assertFalse(Objects.isNull(value));
    }

    private String extractDeepestLeveleValue(Map<String, Object> objectMap) {
        return (String)
                ((Map)
                ((Map)
                getMedicalRecordProperty(objectMap)
                        .get(BaseGenerator.PROPERTIES))
                        .get("medicalRecordColumn"))
                        .get("type");
    }

    private String extractKeywordType(Map<String, Object> objectMap) {
        return (String)
                ((Map)
                ((Map)
                ((Map)
                ((Map)
                getMedicalRecordProperty(objectMap)
                        .get(BaseGenerator.PROPERTIES))
                        .get("medicalRecordColumn"))
                        .get("fields"))
                        .get("keyword"))
                        .get("type");
    }

    private Map getMedicalRecordProperty(Map<String, Object> objectMap) {
        return (Map)
                ((Map)
                ((Map)
                ((Map) objectMap
                        .get(BaseGenerator.PROPERTIES))
                        .get(BaseGenerator.DSM_OBJECT))
                        .get(BaseGenerator.PROPERTIES))
                        .get("medicalRecords");
    }


    private static class TestMappingGenerator extends MappingGenerator {

        public TestMappingGenerator(Parser typeParser, GeneratorPayload generatorPayload) {
            super(typeParser, generatorPayload);
        }

        @Override
        protected DBElement getDBElement() {
            return TestPatchUtil.getColumnNameMap().get(getNameValue().getName());
        }

        public static TestMappingGenerator of(GeneratorPayload generatorPayload) {
            return new TestMappingGenerator(new TypeParser(), generatorPayload);
        }
    }
}