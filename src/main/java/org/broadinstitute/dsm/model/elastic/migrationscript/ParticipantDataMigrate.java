package org.broadinstitute.dsm.model.elastic.migrationscript;


import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.broadinstitute.dsm.db.dao.ddp.participant.ParticipantDataDao;
import org.broadinstitute.dsm.db.dto.ddp.participant.ParticipantDataDto;
import org.broadinstitute.dsm.model.NameValue;
import org.broadinstitute.dsm.model.elastic.Util;
import org.broadinstitute.dsm.model.elastic.export.ElasticMappingExportAdapter;
import org.broadinstitute.dsm.model.elastic.export.UpsertMappingRequestPayload;
import org.broadinstitute.dsm.model.elastic.export.generate.GeneratorPayload;
import org.broadinstitute.dsm.model.elastic.export.generate.MappingGenerator;
import org.broadinstitute.dsm.model.elastic.export.parse.BaseParser;
import org.broadinstitute.dsm.model.elastic.export.parse.TypeParser;
import org.broadinstitute.dsm.statics.ESObjectConstants;

import static org.broadinstitute.dsm.model.elastic.export.generate.BaseGenerator.DSM_OBJECT;
import static org.broadinstitute.dsm.model.elastic.export.generate.BaseGenerator.PROPERTIES;

public class ParticipantDataMigrate extends BaseMigrator {

    private ParticipantDataDao participantDataDao;

    public ParticipantDataMigrate(String index, String realm) {
        super(index, realm, ESObjectConstants.PARTICIPANT_DATA, "participantDataId", ParticipantDataDto.class);
        participantDataDao = new ParticipantDataDao();
    }

    @Override
    protected Map<String, List<Object>> getDataByRealm() {

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

        try {
            Field dataField = ParticipantDataDto.class.getDeclaredField("data");
            BaseParser typeParser = new TypeParser();
            dataField.setAccessible(true);
            for (Map.Entry<String, List<Object>> entry : dataByRealm.entrySet()) {
                String participantId = entry.getKey();
                System.out.println(participantId);
                List<Object> datas = entry.getValue();
                for (Object data : datas) {
                    NameValue nameValue = new NameValue("d.data", dataField.get(data));
                    GeneratorPayload generatorPayload = new GeneratorPayload(nameValue, 0);
                    MappingGenerator mappingGenerator = new MappingGenerator(typeParser, generatorPayload);


                    mapping.merge("properties", mappingGenerator.generate(), (prev, curr) -> {
                        ((Map) prev).merge()
                    });

                    ParticipantDataDto pDataDto = (ParticipantDataDto) data;
                    String json = (String) dataField.get(pDataDto);
                    ObjectMapper objectMapper = new ObjectMapper();
                    try {
                        Map<String, Object> map = objectMapper.readValue(json, Map.class);
                        for (Map.Entry<String, Object> dynamicField : map.entrySet()) {
                            String camelCaseKey = Util.underscoresToCamelCase(dynamicField.getKey());


                            mapping.put(camelCaseKey, typeParser.parse(String.valueOf(dynamicField.getValue())));
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }


        Map<String, Object> innerMap = Map.of(PROPERTIES, mapping, "type", "nested");
        Map<String, Object> objectLevel = Map.of("participantData", innerMap);
        Map<String, Object> dsmLevelProperties = Map.of(PROPERTIES, objectLevel);
        Map<String, Map<String, Object>> dsmLevel = Map.of(DSM_OBJECT, dsmLevelProperties);
        Map<String, Object> finalMapping = Map.of(PROPERTIES, dsmLevel);

        ElasticMappingExportAdapter mappingExporter = new ElasticMappingExportAdapter(new UpsertMappingRequestPayload("participants_structured.atcp.atcp"), finalMapping);
        mappingExporter.export();
    }
}
