package org.broadinstitute.dsm.model.elastic.export.parse;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.dsm.db.structure.DbDateConversion;
import org.broadinstitute.dsm.model.elastic.export.generate.BaseGenerator;

public abstract class BaseParser implements Parser {

    protected BaseGenerator.PropertyInfo propertyInfo;
    protected String fieldName;
    protected String realm;

    public void setPropertyInfo(BaseGenerator.PropertyInfo propertyInfo) {
        this.propertyInfo = propertyInfo;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    @Override
    public Object parse(String element) {
        Class<?> propertyClass = propertyInfo.getPropertyClass();
        Object elementMap;
        try {
            Field field = propertyClass.getDeclaredField(element);
            if (long.class.isAssignableFrom(field.getType())) {
                elementMap = forNumeric(element);
            } else if (boolean.class.isAssignableFrom(field.getType())) {
                elementMap = forBoolean(element);
            } else {
                // either text or date in string
                if (field.getAnnotation(DbDateConversion.class) != null) {
                    elementMap = forDate(element);
                } else {
                    elementMap = forString(element);
                }
            }
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
        return elementMap;
    }

    protected abstract Object forNumeric(String value);

    protected abstract Object forBoolean(String value);

    protected abstract Object forDate(String value);

    protected abstract Object forString(String value);

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

    protected boolean isBoolean(String value) {
        return convertBoolean(value).equalsIgnoreCase(Boolean.TRUE.toString()) ||
               convertBoolean(value).equalsIgnoreCase(Boolean.FALSE.toString());
    }

    public String convertBoolean(String value) {
        if ("'1'".equals(value) || "NOT'0'".equals(value) || "'true'".equals(value)) {
            return "true";
        } else if ("'0'".equals(value) || "NOT'1'".equals(value) || "'false'".equals(value)) {
            return "false";
        } else {
            return value;
        }
    }

    public String convertString(String value) {
        if (isWrappedByChar(value))
            return value.substring(1, value.length() - 1);
        return value;
    }

    protected boolean isWrappedByChar(String value) {
        return StringUtils.isNotBlank(value) && value.charAt(0) == '\'' && value.charAt(value.length() - 1) == '\'';
    }
}
