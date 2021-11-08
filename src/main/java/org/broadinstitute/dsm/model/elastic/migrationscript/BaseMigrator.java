package org.broadinstitute.dsm.model.elastic.migrationscript;

import java.util.List;
import java.util.Map;

import org.broadinstitute.dsm.db.MedicalRecord;
import org.broadinstitute.dsm.model.elastic.Util;
import org.broadinstitute.dsm.model.elastic.search.ElasticSearch;
import org.broadinstitute.dsm.model.elastic.search.ElasticSearchParticipantDto;
import org.broadinstitute.dsm.model.elastic.search.ElasticSearchable;
import org.broadinstitute.dsm.util.ParticipantUtil;
import spark.utils.StringUtils;

public abstract class BaseMigrator implements BulkExportable {
    private final ElasticSearchable elasticSearch;
    protected String OBJECT = "";
    protected final BulkExportFacade bulkExportFacade;
    protected final String realm;

    public BaseMigrator(String index, String realm, ElasticSearchable elasticSearchable) {
        this.elasticSearch = elasticSearchable;
        bulkExportFacade = new BulkExportFacade(index);
        this.realm = realm;
    }

    private void setPrimaryId(List<Map<String, Object>> transformedList) {
        for(Map<String, Object> map: transformedList) {
            map.put("id", map.get("medicalRecordId"));
        }
    }

    private String getParticipantGuid(String participantId) {
        if (!(ParticipantUtil.isGuid(participantId))) {
            ElasticSearchParticipantDto participantById =
                    elasticSearch.getParticipantById("participants_structured.cmi.cmi-brain", participantId);
            participantId = participantById.getParticipantId();
        }
        return participantId;
    }

    public Map generateSource(List<Map<String, Object>> transformedList) {
        return Map.of("dsm", Map.of(OBJECT, transformedList));
    }

    protected void fillBulkRequestWithTransformedMap(Map<String, List<Object>> participantRecords) {
        for (Map.Entry<String, List<Object>> entry: participantRecords.entrySet()) {
            String participantId = entry.getKey();
            List<Object> medicalRecordList = entry.getValue();
            participantId = getParticipantGuid(participantId);
            if (StringUtils.isBlank(participantId)) continue;
            List<Map<String, Object>> transformedList = Util.transformObjectCollectionToCollectionMap(medicalRecordList, MedicalRecord.class);
            setPrimaryId(transformedList);
            bulkExportFacade.addDataToRequest(generateSource(transformedList), participantId);
        }
    }
}
