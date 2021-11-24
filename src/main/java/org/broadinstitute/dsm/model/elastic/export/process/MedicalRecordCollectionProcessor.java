package org.broadinstitute.dsm.model.elastic.export.process;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.google.gson.Gson;
import org.broadinstitute.dsm.model.elastic.ESDsm;
import org.broadinstitute.dsm.model.elastic.export.generate.Collector;

public class MedicalRecordCollectionProcessor extends CollectionProcessor {
    public MedicalRecordCollectionProcessor(ESDsm esDsm, String propertyName, int recordId,
                                            Collector collector) {
        super(esDsm, propertyName, recordId, collector);
    }

    @Override
    protected List<Map<String, Object>> updateIfExistsOrPut(List<Map<String, Object>> fetchedRecords) {
        for (Map<String, Object> medicalRecord: fetchedRecords) {
            Object followUps = medicalRecord.get("followUps");
            if (!Objects.isNull(followUps)) {
                medicalRecord.put("followUps", new Gson().toJson(followUps));
            }
        }
        return super.updateIfExistsOrPut(fetchedRecords);
    }
}
