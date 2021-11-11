package org.broadinstitute.dsm.model.elastic.migrationscript;


import java.lang.reflect.Field;
import java.util.*;

import org.broadinstitute.dsm.db.dao.ddp.participant.ParticipantDataDao;
import org.broadinstitute.dsm.db.dto.ddp.participant.ParticipantDataDto;
import org.broadinstitute.dsm.model.NameValue;
import org.broadinstitute.dsm.model.elastic.export.ElasticMappingExportAdapter;
import org.broadinstitute.dsm.model.elastic.export.UpsertMappingRequestPayload;
import org.broadinstitute.dsm.model.elastic.export.generate.GeneratorPayload;
import org.broadinstitute.dsm.model.elastic.export.generate.MappingGenerator;
import org.broadinstitute.dsm.model.elastic.export.parse.BaseParser;
import org.broadinstitute.dsm.model.elastic.export.parse.TypeParser;
import org.broadinstitute.dsm.statics.ESObjectConstants;

public class ParticipantDataMigrate extends BaseCollectionMigrator {

    public static final String DATA_WITH_ALIAS = "d.data";
    private ParticipantDataDao participantDataDao;

    public ParticipantDataMigrate(String index, String realm) {
        super(index, realm, ESObjectConstants.PARTICIPANT_DATA, "participantDataId", ParticipantDataDto.class);
        participantDataDao = new ParticipantDataDao();
    }

    @Override
    protected Map<String, Object> getDataByRealm() {
        Map<String, List<ParticipantDataDto>> participantDataByRealm = participantDataDao.getParticipantDataByRealm(realm);
        return (Map) participantDataByRealm;
    }

    @Override
    public void export() {
        exportMapping();
        super.export();
    }

    private void exportMapping() {
        Map<String, Object> mapping = new HashMap<>();
        Map<String, List<Object>> dataByRealm = getDataByRealm();
        BaseParser typeParser = new TypeParser();
        try {
            Field dataField = ParticipantDataDto.class.getDeclaredField(ParticipantDataDao.DATA);
            dataField.setAccessible(true);
            for (Map.Entry<String, List<Object>> entry : dataByRealm.entrySet()) {
                List<Object> participantDatas = entry.getValue();
                for (Object jsonData : participantDatas) {
                    NameValue nameValue = new NameValue(DATA_WITH_ALIAS, dataField.get(jsonData));
                    GeneratorPayload generatorPayload = new GeneratorPayload(nameValue);
                    MappingGenerator mappingGenerator = new MappingGenerator(typeParser, generatorPayload);
                    mapping = mappingGenerator.merge(mapping, mappingGenerator.generate());
                }
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        ElasticMappingExportAdapter mappingExporter = new ElasticMappingExportAdapter(new UpsertMappingRequestPayload(
                index), mapping);
        mappingExporter.export();
    }
}
