package org.broadinstitute.dsm.pubsub;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.broadinstitute.dsm.db.DDPInstance;
import org.broadinstitute.dsm.db.dao.fieldsettings.FieldSettingsDao;
import org.broadinstitute.dsm.db.dto.fieldsettings.FieldSettingsDto;
import org.broadinstitute.dsm.export.ExportToES;
import org.broadinstitute.dsm.export.WorkflowForES;
import org.broadinstitute.dsm.model.Value;
import org.broadinstitute.dsm.model.participant.data.FamilyMemberConstants;
import org.broadinstitute.dsm.statics.ESObjectConstants;
import org.broadinstitute.dsm.db.dao.ddp.participant.ParticipantDataDao;
import org.broadinstitute.dsm.db.dto.ddp.participant.ParticipantDataDto;
import org.broadinstitute.dsm.util.ElasticSearchUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class WorkflowStatusUpdate {
    public static final String STUDY_GUID = "studyGuid";
    public static final String PARTICIPANT_GUID = "participantGuid";
    public static final String MEMBER_TYPE = "MEMBER_TYPE";
    public static final String SELF = "SELF";
    public static final String DSS = "DSS";
    private static final Gson gson = new Gson();

    private static final Logger logger = LoggerFactory.getLogger(ExportToES.class);
    private static final ParticipantDataDao participantDataDao = new ParticipantDataDao();
    private static final FieldSettingsDao fieldSettingsDao = FieldSettingsDao.of();

    public static void updateCustomWorkflow(Map<String, String> attributesMap, String data) {
        WorkflowPayload workflowPayload = gson.fromJson(data, WorkflowPayload.class);
        String workflow = workflowPayload.getWorkflow();
        String status = workflowPayload.getStatus();

        String studyGuid = attributesMap.get(STUDY_GUID);
        String ddpParticipantId = attributesMap.get(PARTICIPANT_GUID);
        DDPInstance instance = DDPInstance.getDDPInstanceByGuid(studyGuid);

        List<ParticipantDataDto> participantDatas = participantDataDao.getParticipantDataByParticipantId(ddpParticipantId);
        Optional<FieldSettingsDto> fieldSetting = fieldSettingsDao.getFieldSettingByColumnNameAndInstanceId(Integer.parseInt(instance.getDdpInstanceId()), workflow);
        if (fieldSetting.isEmpty()) {
            logger.warn("Wrong workflow name");
        } else {
            FieldSettingsDto setting = fieldSetting.get();
            boolean isOldParticipant = participantDatas.stream()
                    .anyMatch(participantDataDto -> participantDataDto.getFieldTypeId().equals(setting.getFieldType())
                            || !participantDataDto.getFieldTypeId().contains(FamilyMemberConstants.GROUP));
            if (isOldParticipant) {
                participantDatas.forEach(participantDataDto -> {
                    updateProbandStatusInDB(workflow, status, participantDataDto, studyGuid);
                });
            } else {
                addNewParticipantDataWithStatus(workflow, status, ddpParticipantId, setting);
            }
            exportToESifNecessary(workflow, status, ddpParticipantId, instance, setting, participantDatas);
        }

    }

    public static void exportToESifNecessary(String workflow, String status, String ddpParticipantId,
                                             DDPInstance instance, FieldSettingsDto setting, List<ParticipantDataDto> participantDatas) {
        String actions = setting.getActions();
        if (actions == null) {
            return;
        }
        Value[] actionsArray =  gson.fromJson(actions, Value[].class);
        for (Value action : actionsArray) {
            if (ESObjectConstants.ELASTIC_EXPORT_WORKFLOWS.equals(action.getType())) {
                if (setting.getFieldType().contains(FamilyMemberConstants.GROUP)) {
                    ElasticSearchUtil.writeWorkflow(WorkflowForES.createInstance(instance, ddpParticipantId, workflow, status));
                } else {
                    Optional<WorkflowForES.StudySpecificData> studySpecificDataOptional = getProbandStudySpecificData(participantDatas);
                    studySpecificDataOptional.ifPresent(studySpecificData -> ElasticSearchUtil.writeWorkflow(WorkflowForES
                            .createInstanceWithStudySpecificData(instance, ddpParticipantId, workflow, status, studySpecificData)));
                }
                break;
            }
        }
    }

    private static Optional<WorkflowForES.StudySpecificData> getProbandStudySpecificData(List<ParticipantDataDto> participantDatas) {
        for (ParticipantDataDto participantData: participantDatas) {
            String data = participantData.getData();
            if (data == null) {
                continue;
            }
            Map<String, String> dataMap = gson.fromJson(data, Map.class);
            if (!dataMap.containsKey(FamilyMemberConstants.LASTNAME) || !dataMap.containsKey(FamilyMemberConstants.FIRSTNAME)
                    || !dataMap.containsKey(FamilyMemberConstants.COLLABORATOR_PARTICIPANT_ID)) {
                logger.warn("Participant data doesn't have necessary fields");
            }
            if(isProband(dataMap)) {
                return Optional.of(new WorkflowForES.StudySpecificData(dataMap.get(FamilyMemberConstants.COLLABORATOR_PARTICIPANT_ID),
                        dataMap.get(FamilyMemberConstants.FIRSTNAME), dataMap.get(FamilyMemberConstants.LASTNAME)));
            }
        }
        return Optional.empty();
    }

    public static int addNewParticipantDataWithStatus(String workflow, String status, String ddpParticipantId, FieldSettingsDto setting) {
        JsonObject dataJsonObject = new JsonObject();
        dataJsonObject.addProperty(workflow, status);
        int participantDataId;
        participantDataId = participantDataDao.create(new ParticipantDataDto(
                ddpParticipantId,
                setting.getDdpInstanceId(),
                setting.getFieldType(),
                dataJsonObject.toString(),
                System.currentTimeMillis(),
                WorkflowStatusUpdate.DSS)
        );
        return participantDataId;
    }

    public static void updateProbandStatusInDB(String workflow, String status, ParticipantDataDto participantDataDto, String studyGuid) {
        String oldData = participantDataDto.getData();
        if (oldData == null) {
            return;
        }
        JsonObject dataJsonObject = gson.fromJson(oldData, JsonObject.class);
        if ((participantDataDto.getFieldTypeId().contains("GROUP") || isProband(gson.fromJson(dataJsonObject, Map.class)))) {
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

    private static boolean isProband(Map<String, String> dataMap) {
        return dataMap.containsKey(MEMBER_TYPE) && dataMap.get(MEMBER_TYPE).equals(SELF);
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
