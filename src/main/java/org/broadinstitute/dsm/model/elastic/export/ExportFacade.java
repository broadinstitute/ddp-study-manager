package org.broadinstitute.dsm.model.elastic.export;

import java.util.Map;

public class ExportFacade {

    BaseExporter exportable;
    Generator generator;
    private ExportFacadePayload exportFacadePayload;

    public ExportFacade(ExportFacadePayload exportFacadePayload) {
        this.exportFacadePayload = exportFacadePayload;
        exportable = new ElasticExportAdapter();
        generator = new MappingGenerator(new TypeParser());
    }

    public void export() {
        upsertMapping();
        upsertData();
    }

    private void upsertMapping() {
        Map<String, Object> mappingToUpsert = generator.generate(exportFacadePayload.getNameValue());
        UpsertMappingRequestPayload upsertMappingRequestPayload = new UpsertMappingRequestPayload(exportFacadePayload.getIndex());
        exportable.setUpsertMappingRequestPayload(upsertMappingRequestPayload);
        exportable.exportMapping(mappingToUpsert);
    }

    private void upsertData() {
        generator = new SourceGenerator();
        Map<String, Object> elasticDataToExport = generator.generate(exportFacadePayload.getNameValue());
        UpsertDataRequestPayload upsertDataRequestPayload = new UpsertDataRequestPayload.Builder(exportFacadePayload.getIndex(),
                exportFacadePayload.getId())
                .withDocAsUpsert(true)
                .withRetryOnConflict(5)
                .build();
        exportable.setUpdateRequestPayload(upsertDataRequestPayload);
        exportable.exportData(elasticDataToExport);
    }


}
