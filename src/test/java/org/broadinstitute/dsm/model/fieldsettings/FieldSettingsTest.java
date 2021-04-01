package org.broadinstitute.dsm.model.fieldsettings;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.broadinstitute.dsm.db.dto.fieldsettings.FieldSettingsDto;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class FieldSettingsTest {

    private static final String acceptanceStatusPossibleValue = "[{\"value\":\"ACCEPTED\",\"name\":\"Accepted\",default:true},{\"value\":\"IN_REVIEW\",\"name\":\"In Review\"},{\"name\":\"More Info Needed\",\"value\":\"MORE_INFO_NEEDED\"},{\"name\":\"Not Accepted\",\"value\":\"NOT_ACCEPTED\"},{\"name\":\"Waitlist\",\"value\":\"WAITLIST\"},{\"name\":\"Pre-review\",\"value\":\"PRE_REVIEW\"}]";
    private static final String activePossibleValue = "[{\"value\":\"ACTIVE\",\"name\":\"Active\"},{\"value\":\"HOLD\",\"name\":\"HOLD\"},{\"value\":\"INACTIVE\",\"name\":\"Inactive\"}]";
    private static final String ethnicityPossibleValue = "[{\"name\":\"Hispanic\",\"value\":\"HISPANIC\",default:true},{\"name\":\"Not Hispanic\",\"value\":\"NOT_HISPANIC\"},{\"name\":\"Unknown or Not Reported\",\"value\":\"UNKNOWN\"}]";
    private static final String acceptanceStatusColumnName = "ACCEPTANCE_STATUS";
    private static final String activeColumnName = "ACTIVE";
    private static final String ethnicityColumnName = "ETHNICITY";

    private static FieldSettings fieldSettings;

    @BeforeClass
    public static void first() {
        fieldSettings = new FieldSettings();
    }

    @Test
    public void testGetDefaultOption() {
        String defaultOption = fieldSettings.getDefaultOption(acceptanceStatusPossibleValue);
        Assert.assertEquals("ACCEPTED", defaultOption);
    }

    @Test
    public void testIsDefaultOption() {
        boolean isDefaultOption = fieldSettings.isDefaultOption(acceptanceStatusPossibleValue);
        Assert.assertTrue(isDefaultOption);
    }

    @Test
    public void testGetDefaultOptions() {
        Map<String, String> defaultOptions = fieldSettings.getColumnsWithDefaultOptions(createStaticFieldSettingDtoList());
        Assert.assertEquals("ACCEPTED", defaultOptions.get(acceptanceStatusColumnName));
        Assert.assertEquals("HISPANIC", defaultOptions.get(ethnicityColumnName));
        Assert.assertEquals(null, defaultOptions.get(activeColumnName));
    }

    List<FieldSettingsDto> createStaticFieldSettingDtoList() {
        FieldSettingsDto fieldSettingsDto1 = new FieldSettingsDto();
        fieldSettingsDto1.setColumnName(acceptanceStatusColumnName);
        fieldSettingsDto1.setPossibleValues(acceptanceStatusPossibleValue);
        FieldSettingsDto fieldSettingsDto2 = new FieldSettingsDto();
        fieldSettingsDto2.setColumnName(activeColumnName);
        fieldSettingsDto2.setPossibleValues(activePossibleValue);
        FieldSettingsDto fieldSettingsDto3 = new FieldSettingsDto();
        fieldSettingsDto3.setColumnName(ethnicityColumnName);
        fieldSettingsDto3.setPossibleValues(ethnicityPossibleValue);

        return Arrays.asList(fieldSettingsDto1, fieldSettingsDto2, fieldSettingsDto3);
    }
}