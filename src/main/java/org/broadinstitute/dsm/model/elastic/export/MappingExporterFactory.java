package org.broadinstitute.dsm.model.elastic.export;

import org.broadinstitute.dsm.model.elastic.export.generate.BaseGenerator;

public class MappingExporterFactory implements ExportableFactory {

    @Override
    public Exportable make(BaseGenerator.PropertyInfo propertyInfo) {
        BaseExporter exporter = new NullObjectExporter();
        if (!propertyInfo.getFieldName().equals("followUps")) {
            exporter = new ElasticMappingExportAdapter();
        }
        return exporter;
    }
}
