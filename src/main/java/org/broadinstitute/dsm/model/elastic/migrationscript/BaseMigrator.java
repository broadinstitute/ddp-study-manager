package org.broadinstitute.dsm.model.elastic.migrationscript;

import java.util.List;
import java.util.Map;

import org.broadinstitute.dsm.model.elastic.Util;
import org.broadinstitute.dsm.model.elastic.export.Exportable;
import org.broadinstitute.dsm.model.elastic.export.generate.Generator;
import org.broadinstitute.dsm.model.elastic.search.ElasticSearch;
import org.broadinstitute.dsm.model.elastic.search.ElasticSearchable;
import org.broadinstitute.dsm.statics.ESObjectConstants;
import spark.utils.StringUtils;

public abstract class BaseMigrator implements Exportable, Generator {

    private final ElasticSearchable elasticSearch;
    protected String object;
    protected final BulkExportFacade bulkExportFacade;
    private String primaryId;
    protected final String realm;
    protected final String index;
    private Class aClass;
    private List<Map<String, Object>> transformedList;

    public BaseMigrator(String index, String realm, String object, String primaryId, Class aClass) {
        bulkExportFacade = new BulkExportFacade(index);
        this.primaryId = primaryId;
        this.elasticSearch = new ElasticSearch();
        this.realm = realm;
        this.index = index;
        this.object = object;
        this.aClass = aClass;
    }

    private void setPrimaryId() {
        for(Map<String, Object> map: transformedList) {
            map.put(Util.ID, map.get(primaryId));
        }
    }

    @Override
    public Map<String, Object> generate() {
        return Map.of(ESObjectConstants.DSM, Map.of(object, transformedList));
    }

    protected void fillBulkRequestWithTransformedMap(Map<String, List<Object>> participantRecords) {
        for (Map.Entry<String, List<Object>> entry: participantRecords.entrySet()) {
            String participantId = entry.getKey();
            List<Object> recordList = entry.getValue();
            participantId = getParticipantGuid(participantId, index);
            if (StringUtils.isBlank(participantId)) continue;
            transformedList = Util.transformObjectCollectionToCollectionMap(recordList);
            setPrimaryId();
            bulkExportFacade.addDataToRequest(generate(), participantId);
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
