package org.broadinstitute.dsm.model.elastic.export.generate;

public class MappingGeneratorFactory implements GeneratorFactory {
    @Override
    public BaseGenerator make(BaseGenerator.PropertyInfo propertyInfo) {
        BaseGenerator generator;

        if (propertyInfo.isCollection()) {
            if ("additionalValuesJson".equals(propertyInfo.getPropertyName()) || "data".equals(propertyInfo.getPropertyName())) {
                generator = new CollectionMappingGenerator(new DynamicFieldsMappingGenerator());
            } else {
                generator = new CollectionMappingGenerator();
            }
        } else {
            if ("additionalValuesJson".equals(propertyInfo.getPropertyName()) || "data".equals(propertyInfo.getPropertyName())) {
                generator = new SingleMappingGenerator(new DynamicFieldsMappingGenerator());
            } else {
                generator = new SingleMappingGenerator();
            }
        }
        return generator;
    }
}
