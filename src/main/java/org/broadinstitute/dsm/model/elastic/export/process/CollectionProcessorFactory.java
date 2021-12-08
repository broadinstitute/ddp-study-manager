package org.broadinstitute.dsm.model.elastic.export.process;

import org.broadinstitute.dsm.model.elastic.export.generate.BaseGenerator;
import org.broadinstitute.dsm.statics.ESObjectConstants;

public class CollectionProcessorFactory implements ProcessorFactory {

    @Override
    public BaseProcessor make(BaseGenerator.PropertyInfo propertyInfo) {
        BaseProcessor processor = new CollectionProcessor();
        if (propertyInfo.isCollection()) {
            if (ESObjectConstants.MEDICAL_RECORD.equals(propertyInfo.getPropertyName())) {
                processor = new MedicalRecordCollectionProcessor();
            }
        }
        return processor;
    }
}
