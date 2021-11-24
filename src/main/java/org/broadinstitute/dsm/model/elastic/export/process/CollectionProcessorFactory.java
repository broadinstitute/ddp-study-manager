package org.broadinstitute.dsm.model.elastic.export.process;

import org.broadinstitute.dsm.model.elastic.export.generate.BaseGenerator;

public class CollectionProcessorFactory implements ProcessorFactory {

    @Override
    public BaseProcessor make(BaseGenerator.PropertyInfo propertyInfo) {
        BaseProcessor processor = new CollectionProcessor();
        if (propertyInfo.isCollection()) {
            if ("followUps".equals(propertyInfo.getFieldName())) {
                processor = new MedicalRecordCollectionProcessor();
            }
        }
        return processor;
    }
}
