package org.broadinstitute.dsm.model.elastic.export.parse;

import static org.broadinstitute.dsm.model.elastic.export.parse.TypeParser.*;
import static org.junit.Assert.*;

import org.broadinstitute.dsm.db.MedicalRecord;
import org.broadinstitute.dsm.model.elastic.export.generate.BaseGenerator;
import org.broadinstitute.dsm.model.elastic.migration.DynamicFieldsTypeParser;
import org.junit.Assert;
import org.junit.Test;

public class TypeParserTest {


    @Test
    public void parse() {

        BaseGenerator.PropertyInfo propertyInfo = new BaseGenerator.PropertyInfo(MedicalRecord.class, true);
        propertyInfo.setFieldName("additionalValuesJson");

        DynamicFieldsTypeParser typeParser = new DynamicFieldsTypeParser();
        typeParser.setPropertyInfo(propertyInfo);
        typeParser.setFieldName("scooby");
        typeParser.setDisplayType("TEXT");
        Object parsedObject = typeParser.parse();
        Assert.assertEquals(TEXT_KEYWORD_MAPPING, parsedObject);

        typeParser.setDisplayType("NUMBER");
        Object parsedObject1 = typeParser.parse();
        Assert.assertEquals(LONG_MAPPING, parsedObject1);

        typeParser.setDisplayType("DATE");
        Object parsedObject2 = typeParser.parse();
        Assert.assertEquals(DATE_MAPPING, parsedObject2);

        typeParser.setDisplayType("CHECKBOX");
        Object parsedObject3 = typeParser.parse();
        Assert.assertEquals(BOOLEAN_MAPPING, parsedObject3);
    }

}