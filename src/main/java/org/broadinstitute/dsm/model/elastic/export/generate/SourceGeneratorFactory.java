package org.broadinstitute.dsm.model.elastic.export.generate;

public class SourceGeneratorFactory implements GeneratorFactory {

    @Override
    public BaseGenerator make(BaseGenerator.PropertyInfo propertyInfo) {
        return propertyInfo.isCollection()
                ? new CollectionSourceGenerator()
                : new SingleSourceGenerator();
    }
}
