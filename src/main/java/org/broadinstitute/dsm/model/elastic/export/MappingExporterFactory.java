package org.broadinstitute.dsm.model.elastic.export;

import java.util.Objects;

import org.broadinstitute.dsm.model.elastic.export.generate.BaseGenerator;
import org.broadinstitute.dsm.model.elastic.export.generate.CollectionMappingGenerator;
import org.broadinstitute.dsm.model.elastic.export.parse.TypeParser;

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
