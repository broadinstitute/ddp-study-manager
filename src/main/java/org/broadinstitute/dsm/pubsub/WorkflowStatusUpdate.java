package org.broadinstitute.dsm.pubsub;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.broadinstitute.dsm.db.DDPInstance;
import org.broadinstitute.dsm.db.dao.fieldsettings.FieldSettingsDao;
import org.broadinstitute.dsm.db.dao.participant.data.ParticipantDataDao;
import org.broadinstitute.dsm.db.dto.fieldsettings.FieldSettingsDto;
import org.broadinstitute.dsm.db.dto.participant.data.ParticipantDataDto;
import org.broadinstitute.dsm.model.Value;
import org.broadinstitute.dsm.statics.ESObjectConstants;
import org.broadinstitute.dsm.util.ElasticSearchUtil;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public class WorkflowStatusUpdate {
    public static final String STUDY_GUID = "studyGuid";
    public static final String PARTICIPANT_GUID = "participantGuid";
    public static final String MEMBER_TYPE = "MEMBER_TYPE";
    public static final String SELF = "SELF";
    public static final String DSS = "DSS";
    public static final String RGP = "RGP";
    private static final Gson gson = new Gson();

    private static final ParticipantDataDao participantDataDao = new ParticipantDataDao();
    private static final FieldSettingsDao fieldSettingsDao = new FieldSettingsDao();

    public static void updateCustomWorkflow(Map<String, String> attributesMap, String data) {
        WorkflowPayload workflowPayload = gson.fromJson(data, WorkflowPayload.class);
        String workflow = workflowPayload.getWorkflow();
        String status = workflowPayload.getStatus();

        String studyGuid = attributesMap.get(STUDY_GUID);
        String ddpParticipantId = attributesMap.get(PARTICIPANT_GUID);
        DDPInstance instance = DDPInstance.getDDPInstanceByGuid(studyGuid);

        List<ParticipantDataDto> participantDatas = participantDataDao.getParticipantDataByParticipantId(ddpParticipantId);

        if (!participantDatas.isEmpty()) {
            participantDatas.forEach(participantDataDto -> {
                updateProbandStatusInDB(workflow, status, participantDataDto, studyGuid);
            });
        } else {
            addNewParticipantDataWithStatus(workflow, status, ddpParticipantId);
        }

        exportToESifNecessary(workflow, status, ddpParticipantId, instance);
    }

    public static void exportToESifNecessary(String workflow, String status, String ddpParticipantId, DDPInstance instance) {
        Optional<FieldSettingsDto> fieldSetting = fieldSettingsDao.getFieldSettingByColumnName(workflow);
        fieldSetting.ifPresent(setting -> {
            String actions = setting.getActions();
            if (actions != null) {
                Value[] actionsArray =  gson.fromJson(actions, Value[].class);
                for (Value action : actionsArray) {
                    if (ESObjectConstants.ELASTIC_EXPORT_WORKFLOWS.equals(action.getType())) {
                        ElasticSearchUtil.writeWorkflow(instance, ddpParticipantId, workflow, status);
                        break;
                    }
                }
            }
        });
    }

    public static int addNewParticipantDataWithStatus(String workflow, String status, String ddpParticipantId) {
        Optional<FieldSettingsDto> fieldSetting = fieldSettingsDao.getFieldSettingByColumnName(workflow);
        JsonObject dataJsonObject = new JsonObject();
        dataJsonObject.addProperty(workflow, status);
        AtomicInteger participantDataId = new AtomicInteger();
        fieldSetting.ifPresent(setting ->
                participantDataId.set(participantDataDao.create(new ParticipantDataDto(
                        ddpParticipantId,
                        setting.getDdpInstanceId(),
                        setting.getFieldType(),
                        dataJsonObject.toString(),
                        System.currentTimeMillis(),
                        WorkflowStatusUpdate.DSS)
                )));
        return participantDataId.get();
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
