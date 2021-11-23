package org.broadinstitute.dsm.model.elastic.export.generate;

public interface GeneratorFactory {

    Generator make(BaseGenerator.PropertyInfo propertyInfo);

}
