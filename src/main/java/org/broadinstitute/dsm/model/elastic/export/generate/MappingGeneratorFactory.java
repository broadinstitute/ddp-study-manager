package org.broadinstitute.dsm.model.elastic.export.generate;

public class MappingGeneratorFactory implements GeneratorFactory {
    @Override
    public Generator make(BaseGenerator.PropertyInfo propertyInfo) {
        Generator generator = new NullObjectGenerator();
        if (propertyInfo.isCollection()) {
            generator = new CollectionMappingGenerator();
        } else {
            generator = new SingleMappingGenerator();
        }
        return generator;
    }
}
