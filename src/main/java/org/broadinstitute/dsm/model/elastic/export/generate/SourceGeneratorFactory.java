package org.broadinstitute.dsm.model.elastic.export.generate;

public class SourceGeneratorFactory implements GeneratorFactory {

    @Override
    public BaseGenerator make(BaseGenerator.PropertyInfo propertyInfo) {
        BaseGenerator generator;
        if (propertyInfo.isCollection()) {
            generator = new CollectionSourceGenerator();
        } else {
            generator = new SingleSourceGenerator();
        }
        return generator;
    }
}
