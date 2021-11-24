package org.broadinstitute.dsm.model.elastic.export.process;

import org.broadinstitute.dsm.model.elastic.export.generate.BaseGenerator;

public interface ProcessorFactory {
    Processor make(BaseGenerator.PropertyInfo propertyInfo);
}
