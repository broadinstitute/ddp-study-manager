package org.broadinstitute.dsm.model.elastic.export;

import org.apache.commons.lang3.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;

public class TypeParser extends BaseParser {


    @Override
    protected Object forNumeric(String value) {
        return "long";
    }

    @Override
    protected Object forBoolean(String value) {
        return "boolean";
    }

    @Override
    protected Object forDate(String value) {
        return "date";
    }

    @Override
    protected Object forString(String value) {
        return "text";
    }
}