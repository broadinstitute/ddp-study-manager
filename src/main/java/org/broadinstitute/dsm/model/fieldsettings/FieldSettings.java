package org.broadinstitute.dsm.model.fieldsettings;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import lombok.NonNull;
import org.broadinstitute.dsm.db.dto.fieldsettings.FieldSettingsDto;

public class FieldSettings {

    public static final String KEY_DEFAULT = "default";
    public static final String KEY_VALUE = "value";

    public Map<String, String> getColumnsWithDefaultOptions(@NonNull List<FieldSettingsDto> fieldSettingsDtos) {
        Map<String, String> defaultOptions = new HashMap<>();
        for (FieldSettingsDto fieldSettingDto: fieldSettingsDtos) {
            if (isDefaultOption(fieldSettingDto.getPossibleValues())) {
                defaultOptions.put(fieldSettingDto.getColumnName(), getDefaultOption(fieldSettingDto.getPossibleValues()));
            }
        }
        return defaultOptions;
    }

    String getDefaultOption(String possibleValuesJson) {
        List<Map<String, Object>> possibleValues = new Gson().fromJson(possibleValuesJson, List.class);

        return (String) possibleValues.stream()
                .filter(f -> ((Boolean) f.getOrDefault(KEY_DEFAULT, false)))
                .findFirst()
                .map(m -> m.getOrDefault(KEY_VALUE, ""))
                .orElse("");
    }

    boolean isDefaultOption(String possibleValuesJson) {
         List<Map<String, Object>> possibleValues = new Gson().fromJson(possibleValuesJson, List.class);
         boolean isDefault = false;
         for (Map<String, Object> opt: possibleValues) {
             boolean hasDefault = opt.containsKey(KEY_DEFAULT);
             if (hasDefault) {
                 isDefault = (Boolean) opt.get(KEY_DEFAULT);
             }
         }
         return isDefault;
    }

}
