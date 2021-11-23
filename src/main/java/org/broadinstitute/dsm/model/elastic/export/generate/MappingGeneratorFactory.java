package org.broadinstitute.dsm.model.elastic.export.generate;

public class MappingGeneratorFactory implements GeneratorFactory {
    @Override
    public BaseGenerator make(BaseGenerator.PropertyInfo propertyInfo) {
        BaseGenerator generator;
        if (propertyInfo.isCollection()) {
            generator = new CollectionMappingGenerator();
        } else {
            generator = new SingleMappingGenerator();
        }
        return generator;
    }
}
