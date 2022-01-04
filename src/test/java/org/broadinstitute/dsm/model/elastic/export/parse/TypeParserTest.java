package org.broadinstitute.dsm.model.elastic.export.parse;

import static org.broadinstitute.dsm.model.elastic.export.parse.TypeParser.*;
import static org.junit.Assert.*;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.dsm.db.MedicalRecord;
import org.broadinstitute.dsm.model.NameValue;
import org.broadinstitute.dsm.model.elastic.export.ExportFacadePayload;
import org.broadinstitute.dsm.model.elastic.export.generate.BaseGenerator;
import org.broadinstitute.dsm.model.elastic.export.generate.GeneratorPayload;
import org.broadinstitute.dsm.model.elastic.migration.DynamicFieldsTypeParser;
import org.junit.Assert;
import org.junit.Test;

public class TypeParserTest {


    @Test
    public void parseDynamicFields() {

        BaseGenerator.PropertyInfo propertyInfo = new BaseGenerator.PropertyInfo(MedicalRecord.class, true);
        propertyInfo.setFieldName("additionalValuesJson");

        DynamicFieldsTypeParser typeParser = new DynamicFieldsTypeParser();
        typeParser.setPropertyInfo(propertyInfo);
        typeParser.setFieldName("scooby");
        typeParser.setDisplayType("TEXT");
        Object parsedObject = typeParser.parse(StringUtils.EMPTY);
        Assert.assertEquals(TEXT_KEYWORD_MAPPING, parsedObject);

        typeParser.setDisplayType("NUMBER");
        Object parsedObject1 = typeParser.parse(StringUtils.EMPTY);
        Assert.assertEquals(LONG_MAPPING, parsedObject1);

        typeParser.setDisplayType("DATE");
        Object parsedObject2 = typeParser.parse(StringUtils.EMPTY);
        Assert.assertEquals(DATE_MAPPING, parsedObject2);

        typeParser.setDisplayType("CHECKBOX");
        Object parsedObject3 = typeParser.parse(StringUtils.EMPTY);
        Assert.assertEquals(BOOLEAN_MAPPING, parsedObject3);
    }

    @Test
    public void parseNonDynamicFields() {
        NameValue crRequired = new NameValue("crRequired", true);
        GeneratorPayload generatorPayload = new GeneratorPayload(crRequired);
        ExportFacadePayload exportFacadePayload = new ExportFacadePayload("", "", generatorPayload, "");
        TypeParser typeParser = TypeParserFactory.of(exportFacadePayload);
        Object booleanMapping = typeParser.parse("true");
        assertEquals(BOOLEAN_MAPPING, booleanMapping);
    }

}