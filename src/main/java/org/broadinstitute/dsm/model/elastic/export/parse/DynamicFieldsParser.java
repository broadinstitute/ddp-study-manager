package org.broadinstitute.dsm.model.elastic.export.parse;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.dsm.db.dao.settings.FieldSettingsDao;
import org.broadinstitute.dsm.util.ObjectMapperSingleton;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.broadinstitute.dsm.model.Filter.NUMBER;

public class DynamicFieldsParser extends BaseParser {

    public static final String DATE_TYPE = "DATE";
    public static final String CHECKBOX_TYPE = "CHECKBOX";
    public static final String ACTIVITY_STAFF_TYPE = "ACTIVITY_STAFF";
    public static final String ACTIVITY_TYPE = "ACTIVITY";
    private String displayType;
    private String possibleValuesJson;
    private BaseParser parser;

    public void setDisplayType(String displayType) {
        this.displayType = displayType;
    }

    public void setPossibleValuesJson(String possibleValuesJson) {
        this.possibleValuesJson = possibleValuesJson;
    }

    public void setParser(BaseParser parser) {
        this.parser = parser;
    }

    @Override
    public Object parse(String fieldName) {

        if (StringUtils.isBlank(displayType))
            displayType = FieldSettingsDao.of().getDisplayTypeByInstanceNameAndColumnName(realm, super.fieldName).orElse(StringUtils.EMPTY);

        Object parsedValue;
        if (DATE_TYPE.equals(displayType)) {
            parsedValue = forDate(fieldName);
        } else if (CHECKBOX_TYPE.equals(displayType)) {
            parsedValue = forBoolean(fieldName);
        } else if (isActivityRelatedType()) {
            Optional<String> maybeType = getTypeFromPossibleValuesJson();
            this.displayType = maybeType.orElse(StringUtils.EMPTY);
            parsedValue = maybeType
                    .map(this::parse)
                    .orElse(forString(fieldName));
        } else if (NUMBER.equals(displayType)) {
            parsedValue = forNumeric(fieldName);
        } else {
            parsedValue = forString(fieldName);
        }

        displayType = null;

        return parsedValue;
    }

    @Override
    protected Object forNumeric(String value) {
        return parser.forNumeric(value);
    }

    @Override
    protected Object forBoolean(String value) {
        return parser.forBoolean(value);
    }

    @Override
    protected Object forDate(String value) {
        return parser.forDate(value);
    }

    @Override
    protected Object forString(String value) {
        return parser.forString(value);
    }

    private Optional<String> getTypeFromPossibleValuesJson() {
        try {
            List<Map<String, String>> possibleValues = ObjectMapperSingleton.instance().readValue(possibleValuesJson, new TypeReference<List<Map<String, String>>>() {});
            Optional<String> maybeType = possibleValues.stream()
                    .filter(possibleValue -> possibleValue.containsKey(TYPE))
                    .map(possibleValue -> possibleValue.get(TYPE))
                    .findFirst();
            return maybeType;
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    private boolean isActivityRelatedType() {
        return ACTIVITY_STAFF_TYPE.equals(displayType) || ACTIVITY_TYPE.equals(displayType);
    }
}
