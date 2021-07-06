package org.broadinstitute.dsm.export;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.broadinstitute.dsm.db.DDPInstance;
import org.broadinstitute.dsm.db.dao.ddp.kitrequest.KitRequestDao;
import org.broadinstitute.dsm.db.dto.ddp.kitrequest.ESSamplesDto;
import org.broadinstitute.dsm.statics.ESObjectConstants;
import org.broadinstitute.dsm.util.ElasticSearchUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class SampleExporter implements Exporter {

    private static final Logger logger = LoggerFactory.getLogger(SampleExporter.class);
    private static final ObjectMapper oMapper = new ObjectMapper();
    private static final KitRequestDao kitRequestDao = new KitRequestDao();

    @Override
    public void export(int instanceId) {
        logger.info("Started exporting samples for instance with id " + instanceId);
        List<ESSamplesDto> esSamples = kitRequestDao.getESSamplesByInstanceId(instanceId);
        DDPInstance ddpInstance = DDPInstance.getDDPInstanceById(instanceId);
        if (ddpInstance != null) {
            for (ESSamplesDto sample : esSamples) {
                Map<String, Object> map = oMapper.convertValue(sample, Map.class);
                if (sample.getKitRequestId() != null && sample.getDdpParticipantId() != null) {
                    ElasticSearchUtil.writeSample(ddpInstance, sample.getKitRequestId(), sample.getDdpParticipantId(),
                            ESObjectConstants.SAMPLES, ESObjectConstants.KIT_REQUEST_ID, map);
                }
            }
        }
        logger.info("Finished exporting samples for instance with id " + instanceId);
    }
}
