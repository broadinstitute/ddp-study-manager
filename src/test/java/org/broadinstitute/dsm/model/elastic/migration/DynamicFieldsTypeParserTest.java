package org.broadinstitute.dsm.model.elastic.migration;

import static org.junit.Assert.*;

import java.util.Map;

import org.broadinstitute.dsm.db.dto.settings.FieldSettingsDto;
import org.broadinstitute.dsm.model.elastic.export.generate.MappingGenerator;
import org.broadinstitute.dsm.model.elastic.export.parse.TypeParser;
import org.junit.Test;

public class DynamicFieldsTypeParserTest {

    @Test
    public void parse() {
        String possibleValuesJson = "[{\"value\":\"CONSENT.completedAt\",\"type\":\"DATE\"}]";
        String displayType = "ACTIVITY_STAFF";
        FieldSettingsDto fieldSettingsDto = new FieldSettingsDto.Builder(0)
                .withDisplayType(displayType)
                .withPossibleValues(possibleValuesJson)
                .build();
        DynamicFieldsTypeParser dynamicFieldsTypeParser = new DynamicFieldsTypeParser();
        dynamicFieldsTypeParser.setFieldSettingsDto(fieldSettingsDto);
        Map<String, Object> mapping = (Map<String, Object>)dynamicFieldsTypeParser.parse(displayType);
        Object date = mapping.get(MappingGenerator.TYPE);
        assertEquals(TypeParser.DATE, date);
    }

}