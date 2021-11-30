package org.broadinstitute.dsm.model.elastic.migration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.broadinstitute.dsm.db.dao.settings.FieldSettingsDao;
import org.broadinstitute.dsm.db.dto.settings.FieldSettingsDto;
import org.broadinstitute.dsm.model.elastic.Util;
import org.broadinstitute.dsm.model.elastic.export.ElasticMappingExportAdapter;
import org.broadinstitute.dsm.model.elastic.export.Exportable;
import org.broadinstitute.dsm.model.elastic.export.RequestPayload;
import org.broadinstitute.dsm.model.elastic.export.generate.BaseGenerator;
import org.broadinstitute.dsm.model.elastic.export.parse.BaseParser;
import org.broadinstitute.dsm.model.elastic.export.parse.Parser;
import org.broadinstitute.dsm.model.elastic.export.parse.TypeParser;

import static org.broadinstitute.dsm.model.elastic.export.generate.BaseGenerator.DSM_OBJECT;
import static org.broadinstitute.dsm.util.ElasticSearchUtil.PROPERTIES;

public class DynamicFieldsMappingMigrator implements Exportable {

    private final String index;
    private final String study;
    public Parser parser;
    public Map<String, Object> propertyMap;

    private ElasticMappingExportAdapter elasticMappingExportAdapter = new ElasticMappingExportAdapter();

    public DynamicFieldsMappingMigrator(String index, String study) {
        this.index = index;
        this.study = study;
        this.parser = new DynamicFieldsTypeParser();
        this.propertyMap = new HashMap<>();
    }

    @Override
    public void export() {
        FieldSettingsDao fieldSettingsDao = FieldSettingsDao.of();
        List<FieldSettingsDto> fieldSettingsByStudyName = fieldSettingsDao.getFieldSettingsByStudyName(study);
        for (FieldSettingsDto fieldSettingsDto : fieldSettingsByStudyName) {
            String fieldType = fieldSettingsDto.getFieldType();
            BaseGenerator.PropertyInfo propertyInfo = Util.TABLE_ALIAS_MAPPINGS.get(fieldType);
            if (propertyInfo != null)
                buildMapping(fieldSettingsDto, propertyInfo, "additionalValuesJson");
            else
                buildMapping(fieldSettingsDto, new BaseGenerator.PropertyInfo("participantData", true), "data");
        }
        elasticMappingExportAdapter.setRequestPayload(new RequestPayload(index));
        elasticMappingExportAdapter.setSource(buildFinalMapping());
        elasticMappingExportAdapter.export();
    }

    private Map<String, Object> buildFinalMapping() {
        Map<String, Object> dsmLevelProperties = new HashMap<>(Map.of(PROPERTIES, propertyMap));
        Map<String, Map<String, Object>> dsmLevel = new HashMap<>(Map.of(DSM_OBJECT, dsmLevelProperties));
        Map<String, Object> finalMap = new HashMap<>(Map.of(PROPERTIES, dsmLevel));
        return finalMap;
    }

    private void buildMapping(FieldSettingsDto fieldSettingsDto, BaseGenerator.PropertyInfo propertyInfo, String dynamicFieldsWrapperName) {
        String columnName = Util.underscoresToCamelCase(fieldSettingsDto.getColumnName());
        String propertyName = propertyInfo.getPropertyName();
        Object typeMap = parser.parse(fieldSettingsDto.getDisplayType());
        if (!(propertyMap.containsKey(propertyName))) {
            Map<String, Object> additionalValuesJson = new HashMap<>(Map.of(dynamicFieldsWrapperName, new HashMap<>(Map.of(PROPERTIES, new HashMap<>(Map.of(columnName, typeMap))))));
            Map<String, Object> wrapperMap = new HashMap<>();
            if (propertyInfo.isCollection()) {
                wrapperMap.put("type", "nested");
            }
            wrapperMap.put(PROPERTIES, additionalValuesJson);
            propertyMap.put(propertyName, wrapperMap);
        } else {
            Map<String, Object> outerMap = (Map<String, Object>) propertyMap.get(propertyName);
            Map<String, Object> outerProperties = (Map<String, Object>) outerMap.get(PROPERTIES);
            Map<String, Object> dynamicFieldsJson = (Map<String, Object>) outerProperties.get(dynamicFieldsWrapperName);
            Map<String, Object> innerProperties = (Map<String, Object>) dynamicFieldsJson.get(PROPERTIES);
            innerProperties.putIfAbsent(columnName, typeMap);
        }
    }

    @Override
    public void setSource(Map<String, Object> source) {

    }

    @Override
    public void setRequestPayload(RequestPayload requestPayload) {

    }

}

class DynamicFieldsTypeParser extends TypeParser {

    @Override
    public Object parse(String value) {
        Object parsedValue;
        if ("DATE".equals(value)) {
            parsedValue = forDate(value);
        } else {
            parsedValue = forString(value);
        }
        return parsedValue;
    }
}
