package org.broadinstitute.dsm.model.elastic.migration;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.broadinstitute.dsm.db.dto.settings.FieldSettingsDto;
import org.broadinstitute.dsm.model.elastic.export.parse.TypeParser;

class DynamicFieldsTypeParser extends TypeParser {

    private FieldSettingsDto fieldSettingsDto;

    public void setFieldSettingsDto(FieldSettingsDto fieldSettingsDto) {
        this.fieldSettingsDto = fieldSettingsDto;
    }

    @Override
    public Object parse(String value) {
        Object parsedValue;
        if ("DATE".equals(value)) {
            parsedValue = forDate(value);
        } else if ("CHECKBOX".equals(value)) {
            parsedValue = forBoolean(value);
        } else if ("ACTIVITY_STAFF".equals(value) || "ACTIVITY".equals(value)) {
            String possibleValuesJson = Objects.requireNonNull(fieldSettingsDto).getPossibleValues();
            try {
                List<Map<String, String>> possibleValues = new ObjectMapper().readValue(possibleValuesJson,
                        new TypeReference<List<Map<String, String>>>() {
                        });
                Optional<String> maybeType = possibleValues.stream()
                        .filter(possibleValue -> possibleValue.containsKey("type"))
                        .map(possibleValue -> possibleValue.get("type"))
                        .findFirst();
                parsedValue = maybeType
                        .map(this::parse)
                        .orElse(forString(value));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            parsedValue = forString(value);
        }
        return parsedValue;
    }
}
