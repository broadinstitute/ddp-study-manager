package org.broadinstitute.dsm.model.elastic.migrationscript;


import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.broadinstitute.dsm.db.dao.ddp.participant.ParticipantDataDao;
import org.broadinstitute.dsm.db.dto.ddp.participant.ParticipantDataDto;
import org.broadinstitute.dsm.statics.ESObjectConstants;

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
        //decorate here
        Map<String, List<Object>> dataByRealm = getDataByRealm();
        try {
            Field dataField = ParticipantDataDto.class.getField("data");
            dataField.setAccessible(true);
            for (Map.Entry<String, List<Object>> entry: dataByRealm.entrySet()) {
                String participantId = entry.getKey();
                List<Object> datas = entry.getValue();
                for (Object data: datas) {
                    ParticipantDataDto pDataDto  = (ParticipantDataDto) data;
                    String json = (String) dataField.get(pDataDto);
                    ObjectMapper objectMapper = new ObjectMapper();
                    try {
                        objectMapper.readValue(json, Map.class);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        super.export();
    }
}
