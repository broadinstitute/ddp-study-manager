package org.broadinstitute.dsm.model.elastic.migration;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.dsm.db.dto.settings.FieldSettingsDto;
import org.broadinstitute.dsm.model.elastic.export.parse.TypeParser;
import org.broadinstitute.dsm.util.ObjectMapperSingleton;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static org.broadinstitute.dsm.model.Filter.NUMBER;

public class DynamicFieldsTypeParser extends TypeParser {

    public static final String DATE_TYPE = "DATE";
    public static final String CHECKBOX_TYPE = "CHECKBOX";
    public static final String ACTIVITY_STAFF_TYPE = "ACTIVITY_STAFF";
    public static final String ACTIVITY_TYPE = "ACTIVITY";
    private FieldSettingsDto fieldSettingsDto;

    public void setFieldSettingsDto(FieldSettingsDto fieldSettingsDto) {
        this.fieldSettingsDto = fieldSettingsDto;
    }

    @Override
    public Object parse() {
        String type = fieldSettingsDto.getDisplayType();
        Object parsedValue;
        if (DATE_TYPE.equals(type)) {
            parsedValue = forDate(type);
        } else if (CHECKBOX_TYPE.equals(type)) {
            parsedValue = forBoolean(type);
        } else if (isActivityRelatedType(type)) {
            String possibleValuesJson = Objects.requireNonNull(fieldSettingsDto).getPossibleValues();
            Optional<String> maybeType = getTypeFromPossibleValuesJson(possibleValuesJson);
            parsedValue = maybeType
                    .map(this::parse)
                    .orElse(forString(type));
        } else if (NUMBER.equals(type)) {
            parsedValue = forNumeric(type);
        } else {
            parsedValue = forString(type);
        }
        return parsedValue;
    }

    private Optional<String> getTypeFromPossibleValuesJson(String possibleValuesJson) {
        try {
            List<Map<String, String>> possibleValues = ObjectMapperSingleton.instance().readValue(possibleValuesJson, new TypeReference<List<Map<String, String>>>() {});
            Optional<String> maybeType = possibleValues.stream()
                    .filter(possibleValue -> possibleValue.containsKey(TYPE))
                    .map(possibleValue -> possibleValue.get(TYPE))
                    .findFirst();
            return maybeType;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isActivityRelatedType(String type) {
        return ACTIVITY_STAFF_TYPE.equals(type) || ACTIVITY_TYPE.equals(type);
    }
}
