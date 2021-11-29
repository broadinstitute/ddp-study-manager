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

import static org.broadinstitute.dsm.model.elastic.export.generate.BaseGenerator.PROPERTIES;

public class MappingGeneratorTest {


    @Test
    public void generateTextType() {
        GeneratorPayload generatorPayload = new GeneratorPayload(
            new NameValue(TestPatchUtil.MEDICAL_RECORD_COLUMN, "value"),
            0
        );
        Map<String, Object> objectMap = TestCollectionMappingGenerator.of(generatorPayload).generate();
        Assert.assertEquals(objectMap.keySet().stream().findFirst().get(), PROPERTIES);
        String type = extractDeepestLeveleValue(objectMap);
        Assert.assertEquals("text", type);
    }

    @Test
    public void generateTextTypeWithFields() {
        GeneratorPayload generatorPayload = new GeneratorPayload(
            new NameValue(TestPatchUtil.MEDICAL_RECORD_COLUMN, "value"),
            0
        );
        Map<String, Object> objectMap = TestCollectionMappingGenerator.of(generatorPayload).generate();
        Assert.assertEquals(objectMap.keySet().stream().findFirst().get(), PROPERTIES);
        String type = extractKeywordType(objectMap);
        Assert.assertEquals("keyword", type);
    }

    @Test
    public void generateBooleanType() {
        GeneratorPayload generatorPayload = new GeneratorPayload(
                new NameValue(TestPatchUtil.MEDICAL_RECORD_COLUMN, "true"),
                0
        );
        Map<String, Object> objectMap = TestCollectionMappingGenerator.of(generatorPayload).generate();
        Assert.assertEquals(objectMap.keySet().stream().findFirst().get(), PROPERTIES);
        String type = extractDeepestLeveleValue(objectMap);
        Assert.assertEquals("boolean", type);
    }

    @Test
    public void generateDateType() {
        GeneratorPayload generatorPayload = new GeneratorPayload(
                new NameValue(TestPatchUtil.MEDICAL_RECORD_COLUMN, "2021-10-30"),
                0
        );
        Map<String, Object> objectMap = TestCollectionMappingGenerator.of(generatorPayload).generate();
        Assert.assertEquals(objectMap.keySet().stream().findFirst().get(), PROPERTIES);
        String type = extractDeepestLeveleValue(objectMap);
        Assert.assertEquals("date", type);
    }

    @Test
    public void generateNestedType() {
        GeneratorPayload generatorPayload = new GeneratorPayload(
                new NameValue(TestPatchUtil.MEDICAL_RECORD_COLUMN, "2021-10-30"),
                100
        );
        Map<String, Object> objectMap = TestCollectionMappingGenerator.of(generatorPayload).generate();
        Assert.assertEquals(MappingGenerator.NESTED, getMedicalRecordProperty(objectMap).get(MappingGenerator.TYPE));
    }

    @Test
    public void generateMapping() {
        GeneratorPayload generatorPayload = new GeneratorPayload(
                new NameValue(TestPatchUtil.MEDICAL_RECORD_COLUMN, "2021-10-30"),
                100
        );
        MappingGenerator generator = TestCollectionMappingGenerator.of(generatorPayload);
        Map<String, Object> resultMap = generator.generate();
        Map<String, Object> dsmLevelProperty = Map.of(generator.getOuterPropertyByAlias().getPropertyName(), Map.of(
                MappingGenerator.TYPE, MappingGenerator.NESTED,
                PROPERTIES, Map.of(Util.underscoresToCamelCase(TestPatchUtil.MEDICAL_RECORD_COLUMN), Map.of(MappingGenerator.TYPE, "date"),
                        Util.ID, Map.of(MappingGenerator.TYPE, MappingGenerator.TYPE_KEYWORD)
                        )));
        Map<String, Object> dsmLevelProperties = Map.of(PROPERTIES, dsmLevelProperty);
        Map<String, Object> dsmLevel = Map.of(MappingGenerator.DSM_OBJECT, dsmLevelProperties);
        Map<String, Object> topLevel = Map.of(PROPERTIES, dsmLevel);
        Assert.assertTrue(topLevel.equals(resultMap));
    }

    @Test
    public void merge() {
        NameValue nameValue = new NameValue("d.data", "{\"DDP_INSTANCE\": \"TEST\"}");
        GeneratorPayload generatorPayload = new GeneratorPayload(nameValue, 0);
        TypeParser parser = new TypeParser();
        MappingGenerator mappingGenerator = new CollectionMappingGenerator(parser, generatorPayload);
        Map<String, Object> base = new HashMap<>();
        base = mappingGenerator.merge(base, mappingGenerator.generate());
        NameValue nameValue2 = new NameValue("d.data", "{\"DDP_INSTANCE1\": \"TEST1\"}");
        GeneratorPayload generatorPayload2 = new GeneratorPayload(nameValue2, 0);
        mappingGenerator = new CollectionMappingGenerator(parser, generatorPayload2);
        base = mappingGenerator.merge(base, mappingGenerator.generate());
        Object value = ((Map) ((Map) ((Map) ((Map) ((Map) base
                .get(PROPERTIES))
                .get(MappingGenerator.DSM_OBJECT))
                .get(PROPERTIES))
                .get(mappingGenerator.getOuterPropertyByAlias().getPropertyName()))
                .get(PROPERTIES))
                .get(Util.underscoresToCamelCase("DDP_INSTANCE1"));
        Object value2 = ((Map) ((Map) ((Map) ((Map) ((Map) base
                .get(PROPERTIES))
                .get(MappingGenerator.DSM_OBJECT))
                .get(PROPERTIES))
                .get(mappingGenerator.getOuterPropertyByAlias().getPropertyName()))
                .get(PROPERTIES))
                .get(Util.underscoresToCamelCase("DDP_INSTANCE"));
        Assert.assertFalse(Objects.isNull(value));
        Assert.assertFalse(Objects.isNull(value2));
    }

    @Test
    public void parseJson() {
        NameValue nameValue = new NameValue("m.additionalValuesJson", "{\"DDP_INSTANCE\": \"TEST\", \"DDP_INSTANCE1\": \"TEST1\"}");
        GeneratorPayload generatorPayload = new GeneratorPayload(nameValue, 0);
        TypeParser parser = new TypeParser();
        MappingGenerator mappingGenerator = new CollectionMappingGenerator(parser, generatorPayload);
        Map<String, Object> parseJson = mappingGenerator.parseJson();
        Map<String, Object> additionalValuesJson = (Map)parseJson.get("additionalValuesJson");
        Assert.assertNotNull(additionalValuesJson);
        Assert.assertEquals(TypeParser.TEXT_KEYWORD_MAPPING,((Map) additionalValuesJson.get(PROPERTIES)).get(Util.underscoresToCamelCase("DDP_INSTANCE")));
        Assert.assertEquals(TypeParser.TEXT_KEYWORD_MAPPING,((Map) additionalValuesJson.get(PROPERTIES)).get(Util.underscoresToCamelCase("DDP_INSTANCE1")));
    }


    private String extractDeepestLeveleValue(Map<String, Object> objectMap) {
        return (String)
                ((Map)
                ((Map)
                getMedicalRecordProperty(objectMap)
                        .get(PROPERTIES))
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
                        .get(PROPERTIES))
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
                        .get(PROPERTIES))
                        .get(BaseGenerator.DSM_OBJECT))
                        .get(PROPERTIES))
                        .get("medicalRecords");
    }


    private static class TestSingleMappingGenerator extends SingleMappingGenerator {

        public TestSingleMappingGenerator(Parser parser, GeneratorPayload generatorPayload) {
            super(parser, generatorPayload);
        }

        @Override
        protected DBElement getDBElement() {
            return TestPatchUtil.getColumnNameMap().get(getNameValue().getName());
        }

        public static TestSingleMappingGenerator of(GeneratorPayload generatorPayload) {
            return new TestSingleMappingGenerator(new TypeParser(), generatorPayload);
        }

    }


    private static class TestCollectionMappingGenerator extends CollectionMappingGenerator {

        public TestCollectionMappingGenerator(Parser typeParser, GeneratorPayload generatorPayload) {
            super(typeParser, generatorPayload);
        }

        @Override
        protected DBElement getDBElement() {
            return TestPatchUtil.getColumnNameMap().get(getNameValue().getName());
        }

        public static TestCollectionMappingGenerator of(GeneratorPayload generatorPayload) {
            return new TestCollectionMappingGenerator(new TypeParser(), generatorPayload);
        }
    }
}