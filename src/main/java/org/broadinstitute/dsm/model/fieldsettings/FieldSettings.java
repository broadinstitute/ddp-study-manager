package org.broadinstitute.dsm.model.fieldsettings;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.google.gson.Gson;
import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.dsm.db.dto.fieldsettings.FieldSettingsDto;
import org.broadinstitute.dsm.model.Value;
import org.broadinstitute.dsm.statics.ESObjectConstants;

public class FieldSettings {

    public static final String KEY_DEFAULT = "default";
    public static final String KEY_VALUE = "value";

    public Map<String, String> getColumnsWithDefaultOptions(@NonNull List<FieldSettingsDto> fieldSettingsDtos) {
        Map<String, String> defaultOptions = new HashMap<>();
        for (FieldSettingsDto fieldSettingDto: fieldSettingsDtos) {
            if (isDefaultOption(fieldSettingDto.getPossibleValues())) {
                defaultOptions.put(fieldSettingDto.getColumnName(), getDefaultOptionValue(fieldSettingDto.getPossibleValues()));
            }
        }
        return defaultOptions;
    }

    public Map<String, String> getColumnsWithDefaultOptionsFilteredByElasticExportWorkflow(@NonNull List<FieldSettingsDto> fieldSettingsDtos) {
        Map<String, String> defaultOptionsFileredByElasticExportWorkflow = new HashMap<>();
        for (FieldSettingsDto fieldSettingsDto: fieldSettingsDtos) {
            if (isDefaultOption(fieldSettingsDto.getPossibleValues()) && isElasticExportWorkflowType(fieldSettingsDto.getActions())) {
                defaultOptionsFileredByElasticExportWorkflow.put(fieldSettingsDto.getColumnName(), getDefaultOptionValue(fieldSettingsDto.getPossibleValues()));
            }
        }
        return defaultOptionsFileredByElasticExportWorkflow;
    }

    public String getDefaultOptionValue(String possibleValuesJson) {
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

    boolean isElasticExportWorkflowType(String action) {
        if (StringUtils.isBlank(action)) return false;
        Value[] actions = new Gson().fromJson(action, Value[].class);
        return Stream.of(actions)
                .anyMatch(act -> ESObjectConstants.ELASTIC_EXPORT_WORKFLOWS.equals(act.getType()));
    }

}
