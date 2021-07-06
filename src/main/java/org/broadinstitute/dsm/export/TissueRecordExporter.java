package org.broadinstitute.dsm.export;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.broadinstitute.dsm.db.DDPInstance;
import org.broadinstitute.dsm.db.dao.ddp.tissue.ESTissueRecordsDao;
import org.broadinstitute.dsm.db.dto.ddp.tissue.ESTissueRecordsDto;
import org.broadinstitute.dsm.statics.ESObjectConstants;
import org.broadinstitute.dsm.util.ElasticSearchUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class TissueRecordExporter implements Exporter {

    private static final Logger logger = LoggerFactory.getLogger(TissueRecordExporter.class);
    private static final ESTissueRecordsDao esTissueRecordsDao = new ESTissueRecordsDao();
    private static final ObjectMapper oMapper = new ObjectMapper();

    @Override
    public void export(int instanceId) {
        logger.info("Started exporting tissue records for instance with id " + instanceId);
        List<ESTissueRecordsDto> esTissueRecords = TissueRecordExporter.esTissueRecordsDao.getESTissueRecordsByInstanceId(instanceId);
        DDPInstance ddpInstance = DDPInstance.getDDPInstanceById(instanceId);
        if (ddpInstance != null) {
            for (ESTissueRecordsDto tissueRecord : esTissueRecords) {
                Map<String, Object> map = oMapper.convertValue(tissueRecord, Map.class);
                if (tissueRecord.getTissueRecordId() != null && tissueRecord.getDdpParticipantId() != null) {
                    ElasticSearchUtil.writeDsmRecord(ddpInstance, tissueRecord.getTissueRecordId(), tissueRecord.getDdpParticipantId(),
                            ESObjectConstants.TISSUE_RECORDS, ESObjectConstants.TISSUE_RECORDS_ID, map);
                }
            }
        }
        logger.info("Finished exporting tissue records for instance with id " + instanceId);
    }
}
