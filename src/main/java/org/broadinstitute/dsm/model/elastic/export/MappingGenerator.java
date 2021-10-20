package org.broadinstitute.dsm.model.elastic.export;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.dsm.model.NameValue;

public class MappingGenerator extends BaseGenerator {


    @Override
    public Map<String, Object> generate(NameValue nameValue) {
        initializeNecessaryFields(Objects.requireNonNull(nameValue));
        String property = TABLE_ALIAS_MAPPINGS.get(dbElement.getTableAlias());
        String value = (String) nameValue.getValue();
        String type = "text";


        if (StringUtils.isNumeric(value)) {
            type = "integer";
        } else if (isBoolean(value)) {
            type = "boolean";
        } else if (isDateOrTimeOrDateTime(value)) {
            type = "date";
        }

        Map<String, Object> fieldType = Map.of("type", type);
        Map<String, Map<String, Map<String, Object>>> mappedField = Map.of(PROPERTIES, Map.of(dbElement.getColumnName(), fieldType));
        Map<String, Map<String, Map<String, Map<String, Object>>>> dsmLevelProperty = Map.of(property, mappedField);
        Map<String, Object> dsmLevelProperties = Map.of(PROPERTIES, dsmLevelProperty);
        Map<String, Map<String, Object>> dsmLevel = Map.of(DSM_OBJECT, dsmLevelProperties);
        return Map.of(PROPERTIES, dsmLevel);
    }

    private boolean isDateOrTimeOrDateTime(String value) {
        return isDate(value) || isTime(value) || isDateTime(value);
    }

    private boolean isDateTime(String value) {
        try {
            LocalDateTime.parse(value);
        } catch (DateTimeParseException dtpe) {
            return false;
        }
        return true;
    }

    private boolean isTime(String value) {
        try {
            LocalTime.parse(value);
        } catch (DateTimeParseException dtpe) {
            return false;
        }
        return true;
    }

    private boolean isDate(String value) {
        try {
            LocalDate.parse(value);
        } catch (DateTimeParseException dtpe) {
            return false;
        }
        return true;
    }

    private boolean isBoolean(String value) {
        return value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false");
    }


}
