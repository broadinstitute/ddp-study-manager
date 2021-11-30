package org.broadinstitute.dsm.model.elastic.migration;

import java.util.Map;

import org.broadinstitute.dsm.db.dao.settings.FieldSettingsDao;
import org.broadinstitute.dsm.model.elastic.export.ElasticMappingExportAdapter;
import org.broadinstitute.dsm.model.elastic.export.Exportable;
import org.broadinstitute.dsm.model.elastic.export.RequestPayload;

public class DynamicFieldsMappingMigrator implements Exportable {

    private ElasticMappingExportAdapter elasticMappingExportAdapter = new ElasticMappingExportAdapter();

    public DynamicFieldsMappingMigrator(String index, String study) {

    }

    @Override
    public void export() {
        FieldSettingsDao fieldSettingsDao = FieldSettingsDao.of();
        fieldSettingsDao.getFieldSettingsByInstanceId()
        elasticMappingExportAdapter.export();
    }

    @Override
    public void setSource(Map<String, Object> source) {

    }

    @Override
    public void setRequestPayload(RequestPayload requestPayload) {

    }
}
