package org.broadinstitute.dsm.model.elastic.export;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

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
        } else if (isDateTime(value)) {
            type = "date";
        }

        //properties.dsm.properties.medicalRecords -> {"type": "boolean"}
        Map<String, Object> jsonMap = new HashMap<>();
        Map<String, Object> message = new HashMap<>();
        message.put("type", type);
        Map<String, Object> properties = new HashMap<>();
        properties.put("message", message);
        jsonMap.put("properties", properties);
        // Date, time, datetime


        return null;
    }

    private boolean isDateTime(String value) {
        try {
            LocalDate.parse(value);
            LocalTime.parse(value);
            LocalDateTime.parse(value);
        } catch (DateTimeParseException dtpe) {
            return false;
        }
        return true;
    }

    private boolean isBoolean(String value) {
        return value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false");
    }


}
