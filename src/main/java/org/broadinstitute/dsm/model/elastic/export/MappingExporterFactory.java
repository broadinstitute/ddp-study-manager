package org.broadinstitute.dsm.model.elastic.export;

import java.util.Objects;

import org.broadinstitute.dsm.model.elastic.export.generate.BaseGenerator;

public class MappingExporterFactory implements ExportableFactory {


    @Override
    public Exportable make(BaseGenerator.PropertyInfo propertyInfo) {
        BaseExporter exporter = new NullObjectExporter();
        if (Objects.requireNonNull(propertyInfo).isCollection()) {
            if (propertyInfo.getFieldName())
        } else {

        }
        return exporter;
    }
}
