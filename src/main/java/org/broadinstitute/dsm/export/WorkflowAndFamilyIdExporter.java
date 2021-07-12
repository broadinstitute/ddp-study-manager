package org.broadinstitute.dsm.export;

import com.google.gson.Gson;
import org.broadinstitute.dsm.db.DDPInstance;
import org.broadinstitute.dsm.db.dao.ddp.instance.DDPInstanceDao;
import org.broadinstitute.dsm.db.dao.ddp.participant.ParticipantDataDao;
import org.broadinstitute.dsm.db.dao.settings.FieldSettingsDao;
import org.broadinstitute.dsm.db.dto.ddp.participant.ParticipantDataDto;
import org.broadinstitute.dsm.db.dto.settings.FieldSettingsDto;
import org.broadinstitute.dsm.model.Value;
import org.broadinstitute.dsm.model.elasticsearch.ESProfile;
import org.broadinstitute.dsm.model.elasticsearch.ElasticSearch;
import org.broadinstitute.dsm.model.participant.data.FamilyMemberConstants;
import org.broadinstitute.dsm.statics.ESObjectConstants;
import org.broadinstitute.dsm.util.ElasticSearchUtil;
import org.broadinstitute.dsm.util.ParticipantUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class WorkflowAndFamilyIdExporter implements Exporter {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowAndFamilyIdExporter.class);
    private static final Gson gson = new Gson();
    private static final ParticipantDataDao participantDataDao = new ParticipantDataDao();
    public static final String RGP_PARTICIPANTS = "RGP_PARTICIPANTS";

    @Override
    public void export(int instanceId) {

    }

    public void export(int instanceId, AtomicBoolean clearBeforeUpdate) {
        logger.info("Started exporting workflows and family ID-s for instance with id " + instanceId);
        List<String> workFlowColumnNames = findWorkFlowColumnNames(instanceId);
        List<ParticipantDataDto> allParticipantData = participantDataDao.getParticipantDataByInstanceid(instanceId);
        checkWorkflowNamesAndExport(workFlowColumnNames, allParticipantData, instanceId, clearBeforeUpdate);
        logger.info("Finished exporting workflows and family ID-s for instance with id " + instanceId);
    }

    private List<String> findWorkFlowColumnNames(int instanceId) {
        FieldSettingsDao fieldSettingsDao = FieldSettingsDao.of();
        List<FieldSettingsDto> fieldSettings = fieldSettingsDao.getFieldSettingsByInstanceId(instanceId);
        List<String> workflowColumns = new ArrayList<>();
        for (FieldSettingsDto fieldSetting: fieldSettings) {
            String actions = fieldSetting.getActions();
            if (actions != null) {
                Value[] actionsArray =  gson.fromJson(actions, Value[].class);
                for (Value action : actionsArray) {
                    if (ESObjectConstants.ELASTIC_EXPORT_WORKFLOWS.equals(action.getType())) {
                        workflowColumns.add(fieldSetting.getColumnName());
                        break;
                    }
                }
            }
        }
        return workflowColumns;
    }

    public void checkWorkflowNamesAndExport(List<String> workFlowColumnNames, List<ParticipantDataDto> allParticipantData,
                                                   int instanceId, AtomicBoolean clearBeforeUpdate) {
        DDPInstance ddpInstance = DDPInstance.getDDPInstanceById(instanceId);
        Optional<String> maybeEsParticipantIndex =
                new DDPInstanceDao().getEsParticipantIndexByInstanceId(instanceId);
        for (ParticipantDataDto participantData: allParticipantData) {
            String data = participantData.getData();
            if (data == null) {
                continue;
            }
            String ddpParticipantId = participantData.getDdpParticipantId();
            String finalDdpParticipantId = ddpParticipantId;
            List<ParticipantDataDto> participantDataFamily = allParticipantData.stream()
                    .filter(participantDataDto -> participantDataDto.getDdpParticipantId().equals(finalDdpParticipantId))
                    .collect(Collectors.toList());
            if (!ParticipantUtil.isGuid(ddpParticipantId)) {
                ddpParticipantId = getGuidIfWeHaveAltpid(ddpParticipantId, maybeEsParticipantIndex);
            }
            if ("".equals(ddpParticipantId)) {
                continue;
            }
            Map<String, String> dataMap = gson.fromJson(data, Map.class);
            exportWorkflows(workFlowColumnNames, ddpInstance, participantData, ddpParticipantId, participantDataFamily, dataMap, clearBeforeUpdate);
            if (dataMap.containsKey(FamilyMemberConstants.FAMILY_ID)) {
                ElasticSearchUtil.writeDsmRecord(ddpInstance, null,
                        ddpParticipantId, ESObjectConstants.FAMILY_ID, dataMap.get(FamilyMemberConstants.FAMILY_ID), null);
            }
        }
    }

    public String getGuidIfWeHaveAltpid(String ddpParticipantId, Optional<String> maybeEsParticipantIndex) {
        if (maybeEsParticipantIndex.isPresent()) {
            ElasticSearch participantESDataByAltpid =
                    ElasticSearchUtil.getParticipantESDataByAltpid(maybeEsParticipantIndex.get(), ddpParticipantId);
            ddpParticipantId = participantESDataByAltpid.getProfile().map(ESProfile::getParticipantGuid).orElse("");
        } else {
            logger.error("Wrong instance ID");
        }
        return ddpParticipantId;
    }

    public void exportWorkflows(List<String> workFlowColumnNames, DDPInstance ddpInstance, ParticipantDataDto participantData,
                                       String ddpParticipantId, List<ParticipantDataDto> participantDataFamily, Map<String, String> dataMap,
                                       AtomicBoolean clearBeforeUpdate) {
        if (participantData.getFieldTypeId().equals(RGP_PARTICIPANTS)) {
            if (!ParticipantUtil.checkApplicantEmail(dataMap.get(FamilyMemberConstants.COLLABORATOR_PARTICIPANT_ID), participantDataFamily)) {
                return;
            }
            WorkflowForES.StudySpecificData studySpecificData = new WorkflowForES.StudySpecificData(
                    dataMap.get(FamilyMemberConstants.COLLABORATOR_PARTICIPANT_ID),
                    dataMap.get(FamilyMemberConstants.FIRSTNAME),
                    dataMap.get(FamilyMemberConstants.LASTNAME)
            );
            dataMap.entrySet().stream().filter(entry -> workFlowColumnNames.contains(entry.getKey()))
                    .forEach(entry -> {
                        ElasticSearchUtil.writeWorkflow(WorkflowForES.createInstanceWithStudySpecificData(ddpInstance, ddpParticipantId,
                                entry.getKey(), entry.getValue(), studySpecificData), clearBeforeUpdate.get());
                        clearBeforeUpdate.set(false);
                    });
        } else {
            dataMap.entrySet().stream().filter(entry -> workFlowColumnNames.contains(entry.getKey()))
                    .forEach(entry -> {
                        ElasticSearchUtil.writeWorkflow(WorkflowForES.createInstance(ddpInstance, ddpParticipantId,
                                entry.getKey(), entry.getValue()), clearBeforeUpdate.get());
                        clearBeforeUpdate.set(false);
                    });
        }
    }
}
