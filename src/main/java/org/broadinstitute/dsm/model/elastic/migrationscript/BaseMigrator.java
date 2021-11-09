package org.broadinstitute.dsm.model.elastic.migrationscript;

import java.util.List;
import java.util.Map;

import org.broadinstitute.dsm.db.MedicalRecord;
import org.broadinstitute.dsm.model.elastic.Util;
import org.broadinstitute.dsm.model.elastic.export.Exportable;
import org.broadinstitute.dsm.model.elastic.search.ElasticSearch;
import org.broadinstitute.dsm.model.elastic.search.ElasticSearchParticipantDto;
import org.broadinstitute.dsm.model.elastic.search.ElasticSearchable;
import org.broadinstitute.dsm.statics.ESObjectConstants;
import org.broadinstitute.dsm.util.ParticipantUtil;
import spark.utils.StringUtils;

public abstract class BaseMigrator implements Exportable {

    private final ElasticSearchable elasticSearch;
    protected String object;
    protected final BulkExportFacade bulkExportFacade;
    private String primaryId;
    protected final String realm;
    protected final String index;
    private Class aClass;

    public BaseMigrator(String index, String realm, String object, String primaryId, Class aClass) {
        bulkExportFacade = new BulkExportFacade(index);
        this.primaryId = primaryId;
        this.elasticSearch = new ElasticSearch();
        this.realm = realm;
        this.index = index;
        this.object = object;
        this.aClass = aClass;
    }

    private void setPrimaryId(List<Map<String, Object>> transformedList) {
        for(Map<String, Object> map: transformedList) {
            map.put(Util.ID, map.get(primaryId));
        }
    }

    private String getParticipantGuid(String participantId) {
        if (!(ParticipantUtil.isGuid(participantId))) {
            ElasticSearchParticipantDto participantById =
                    elasticSearch.getParticipantById(index, participantId);
            participantId = participantById.getParticipantId();
        }
        return participantId;
    }

    public Map generateSource(List<Map<String, Object>> transformedList) {
        return Map.of(ESObjectConstants.DSM, Map.of(object, transformedList));
    }

    protected void fillBulkRequestWithTransformedMap(Map<String, List<Object>> participantRecords) {
        for (Map.Entry<String, List<Object>> entry: participantRecords.entrySet()) {
            String participantId = entry.getKey();
            List<Object> recordList = entry.getValue();
            participantId = getParticipantGuid(participantId);
            if (StringUtils.isBlank(participantId)) continue;
            List<Map<String, Object>> transformedList = Util.transformObjectCollectionToCollectionMap(recordList, this.aClass);
            setPrimaryId(transformedList);
            bulkExportFacade.addDataToRequest(generateSource(transformedList), participantId);
            System.err.println(participantId); //FOR TESTING
        }
    }

    protected abstract Map<String, List<Object>> getDataByRealm();

    @Override
    public void export() {
        fillBulkRequestWithTransformedMap(getDataByRealm());
        bulkExportFacade.executeBulkUpsert();
    }
}
