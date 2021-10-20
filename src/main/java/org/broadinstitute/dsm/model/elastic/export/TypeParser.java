package org.broadinstitute.dsm.model.elastic.export;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.dsm.model.NameValue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;

public class TypeParser implements Parser {
    public TypeParser() {
    }

    @Override
    public String parseType(NameValue nameValue) {
        String value = (String) nameValue.getValue();
        String type = "text";


        if (StringUtils.isNumeric(value)) {
            type = "integer";
        } else if (isBoolean(value)) {
            type = "boolean";
        } else if (isDateOrTimeOrDateTime(value)) {
            type = "date";
        }
        return type;
    }

    boolean isDateOrTimeOrDateTime(String value) {
        return isDate(value) || isTime(value) || isDateTime(value);
    }

    boolean isDateTime(String value) {
        try {
            LocalDateTime.parse(value);
        } catch (DateTimeParseException dtpe) {
            return false;
        }
        return true;
    }

    boolean isTime(String value) {
        try {
            LocalTime.parse(value);
        } catch (DateTimeParseException dtpe) {
            return false;
        }
        return true;
    }

    boolean isDate(String value) {
        try {
            LocalDate.parse(value);
        } catch (DateTimeParseException dtpe) {
            return false;
        }
        return true;
    }

    boolean isBoolean(String value) {
        return value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false");
    }
}