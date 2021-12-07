package org.broadinstitute.dsm.model.elastic.export.process;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.google.gson.Gson;
import org.broadinstitute.dsm.model.elastic.ESDsm;
import org.broadinstitute.dsm.model.elastic.export.generate.Collector;
import org.broadinstitute.dsm.statics.ESObjectConstants;

public class MedicalRecordCollectionProcessor extends CollectionProcessor {

    public MedicalRecordCollectionProcessor() {

    }

    @Override
    protected List<Map<String, Object>> updateIfExistsOrPut(List<Map<String, Object>> fetchedRecords) {
        for (Map<String, Object> medicalRecord: fetchedRecords) {
            Object followUps = medicalRecord.get(ESObjectConstants.FOLLOW_UPS);
            if (!Objects.isNull(followUps)) {
                medicalRecord.put(ESObjectConstants.FOLLOW_UPS, new Gson().toJson(followUps));
            }
        }
        return super.updateIfExistsOrPut(fetchedRecords);
    }
}
