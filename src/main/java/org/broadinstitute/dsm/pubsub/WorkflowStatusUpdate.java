package org.broadinstitute.dsm.pubsub;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.broadinstitute.dsm.db.DDPInstance;
import org.broadinstitute.dsm.db.dao.ddp.participant.ParticipantDataDao;
import org.broadinstitute.dsm.db.dto.ddp.participant.ParticipantDataDto;
import org.broadinstitute.dsm.util.ElasticSearchUtil;

import java.util.List;
import java.util.Map;

public class WorkflowStatusUpdate {
    public static final String STUDY_GUID = "studyGuid";
    public static final String PARTICIPANT_GUID = "participantGuid";
    public static final String MEMBER_TYPE = "MEMBER_TYPE";
    public static final String SELF = "SELF";
    public static final String DSS = "DSS";
    public static final String RGP = "RGP";
    private static final Gson gson = new Gson();

    private static final ParticipantDataDao participantDataDao = new ParticipantDataDao();

    public static void updateCustomWorkflow(Map<String, String> attributesMap, String data) {
        WorkflowPayload workflowPayload = gson.fromJson(data, WorkflowPayload.class);
        String workflow = workflowPayload.getWorkflow();
        String status = workflowPayload.getStatus();

        String studyGuid = attributesMap.get(STUDY_GUID);
        String ddpParticipantId = attributesMap.get(PARTICIPANT_GUID);
        DDPInstance instance = DDPInstance.getDDPInstanceByGuid(studyGuid);

        List<ParticipantDataDto> participantDatas = participantDataDao.getParticipantDataByParticipantId(ddpParticipantId);

        participantDatas.forEach(participantDataDto -> {
            updateProbandStatusInDB(workflow, status, participantDataDto, studyGuid);
        });

        ElasticSearchUtil.writeWorkflow(instance, ddpParticipantId, workflow, status);
    }

    public static void updateProbandStatusInDB(String workflow, String status, ParticipantDataDto participantDataDto, String studyGuid) {
        String oldData = participantDataDto.getData();
        if (oldData == null) {
            return;
        }
        JsonObject dataJsonObject = gson.fromJson(oldData, JsonObject.class);
        if ((!RGP.equals(studyGuid) || isProband(dataJsonObject)) && dataJsonObject.get(workflow) != null) {
            dataJsonObject.addProperty(workflow, status);
            participantDataDao.updateParticipantDataColumn(
                    new ParticipantDataDto(participantDataDto.getParticipantDataId(),
                            participantDataDto.getDdpParticipantId(),
                            participantDataDto.getDdpInstanceId(),
                            participantDataDto.getFieldTypeId(),
                            dataJsonObject.toString(),
                            System.currentTimeMillis(), DSS)
            );
        }
    }

    private static boolean isProband(JsonObject dataJsonObject) {
        return dataJsonObject.has(MEMBER_TYPE) && dataJsonObject.get(MEMBER_TYPE).getAsString().equals(SELF);
    }

    public static class WorkflowPayload {
        private String workflow;
        private String status;

        public String getWorkflow() {
            return workflow;
        }

        public String getStatus() {
            return status;
        }
    }
}
