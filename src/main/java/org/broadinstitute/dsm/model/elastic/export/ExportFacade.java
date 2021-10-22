package org.broadinstitute.dsm.model.elastic.export;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.broadinstitute.dsm.db.structure.DBElement;
import org.broadinstitute.dsm.model.elastic.ESDsm;
import org.broadinstitute.dsm.model.elastic.Util;
import org.broadinstitute.dsm.model.elastic.search.ElasticSearch;
import org.broadinstitute.dsm.model.elastic.search.ElasticSearchParticipantDto;
import org.broadinstitute.dsm.model.elastic.search.ElasticSearchable;
import org.broadinstitute.dsm.util.PatchUtil;

public class ExportFacade {

    BaseExporter exportable;
    Generator generator;
    ElasticSearchable searchable;
    private ExportFacadePayload exportFacadePayload;

    public ExportFacade(ExportFacadePayload exportFacadePayload) {
        this.exportFacadePayload = Objects.requireNonNull(exportFacadePayload);
        exportable = new ElasticExportAdapter();
        searchable = new ElasticSearch();
    }

    public void export() {
        upsertMapping();
        fetchData();
        upsertData();
    }

    private void upsertMapping() {
        generator = new MappingGenerator(new TypeParser(), exportFacadePayload.getGeneratorPayload());
        Map<String, Object> mappingToUpsert = generator.generate();
        UpsertMappingRequestPayload upsertMappingRequestPayload = new UpsertMappingRequestPayload(exportFacadePayload.getIndex());
        exportable.setUpsertMappingRequestPayload(upsertMappingRequestPayload);
        exportable.exportMapping(mappingToUpsert);
    }

    private void fetchData() {
        ElasticSearchParticipantDto participantById = searchable.getParticipantById(exportFacadePayload.getIndex(), exportFacadePayload.getDocId());
        DBElement dbElement = PatchUtil.getColumnNameMap().get(exportFacadePayload.getGeneratorPayload().getNameValue().getName());
        BaseGenerator.PropertyInfo propertyInfo = Util.TABLE_ALIAS_MAPPINGS.get(dbElement.getTableAlias());

        Field field =
                Arrays.stream(ESDsm.class.getDeclaredFields()).filter(f -> propertyInfo.propertyName.equals(f.getName())).findFirst().get();
        participantById.getDsm().map(esDsm -> {
                    List<Map<String, Object>> o = null;
                    try {
                        o = (List<Map<String, Object>>) field.get(esDsm);
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                    return o;
                });

    }

    private void upsertData() {
        generator = new SourceGenerator(new ValueParser(), exportFacadePayload.getGeneratorPayload());
        Map<String, Object> elasticDataToExport = generator.generate();
        UpsertDataRequestPayload upsertDataRequestPayload = new UpsertDataRequestPayload.Builder(exportFacadePayload.getIndex(),
                exportFacadePayload.getDocId())
                .withDocAsUpsert(true)
                .withRetryOnConflict(5)
                .build();
        exportable.setUpdateRequestPayload(upsertDataRequestPayload);
        exportable.exportData(elasticDataToExport);
    }


}
