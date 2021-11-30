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

    private ElasticMappingExportAdapter elasticMappingExportAdapter = new ElasticMappingExportAdapter();

    public DynamicFieldsMappingMigrator(String index, String study) {
        this.index = index;
        this.study = study;
    }

    @Override
    public void export() {
        FieldSettingsDao fieldSettingsDao = FieldSettingsDao.of();
        List<FieldSettingsDto> fieldSettingsByStudyName = fieldSettingsDao.getFieldSettingsByStudyName(study);
        for (FieldSettingsDto fieldSettingsDto : fieldSettingsByStudyName) {
            String fieldType = fieldSettingsDto.getFieldType();
            // oD, m,t
            BaseGenerator.PropertyInfo propertyInfo = Util.TABLE_ALIAS_MAPPINGS.get(fieldType);
            if (propertyInfo != null) {
                String propertyName = propertyInfo.getPropertyName();
                new HashMap<>(Map.of(propertyName, new HashMap<>(Map.of(PROPERTIES,""))));
                Map<String, Object> dsmLevelProperties = new HashMap<>(Map.of(PROPERTIES, objectLevel));
                Map<String, Map<String, Object>> dsmLevel = new HashMap<>(Map.of(DSM_OBJECT, dsmLevelProperties));
                Map<String, Object> finalMap = new HashMap<>(Map.of(PROPERTIES, dsmLevel));
            }


        }
        elasticMappingExportAdapter.export();
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

        switch (value) {

        }
    }
}
