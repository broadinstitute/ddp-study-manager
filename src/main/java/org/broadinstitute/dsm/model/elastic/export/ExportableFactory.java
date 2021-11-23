package org.broadinstitute.dsm.model.elastic.export;

import org.broadinstitute.dsm.model.elastic.export.generate.BaseGenerator;

public interface ExportableFactory {

    Exportable make(BaseGenerator.PropertyInfo propertyInfo);

}
