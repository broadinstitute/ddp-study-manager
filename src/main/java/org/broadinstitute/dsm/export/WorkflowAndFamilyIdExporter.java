package org.broadinstitute.dsm.export;

import com.google.gson.Gson;
import org.broadinstitute.ddp.db.TransactionWrapper;
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
import org.broadinstitute.dsm.statics.ApplicationConfigConstants;
import org.broadinstitute.dsm.statics.ESObjectConstants;
import org.broadinstitute.dsm.util.ElasticSearchUtil;
import org.broadinstitute.dsm.util.ParticipantUtil;
import org.elasticsearch.client.RestHighLevelClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
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
        try (RestHighLevelClient client = ElasticSearchUtil.getClientForElasticsearchCloud(TransactionWrapper.getSqlFromConfig(ApplicationConfigConstants.ES_URL),
                TransactionWrapper.getSqlFromConfig(ApplicationConfigConstants.ES_USERNAME), TransactionWrapper.getSqlFromConfig(ApplicationConfigConstants.ES_PASSWORD))) {

            logger.info("Started exporting workflows and family ID-s for instance with id " + instanceId);
            List<String> workFlowColumnNames = findWorkFlowColumnNames(instanceId);
            List<ParticipantDataDto> allParticipantData = participantDataDao.getParticipantDataByInstanceid(instanceId);
            checkWorkflowNamesAndExport(client, workFlowColumnNames, allParticipantData, instanceId, clearBeforeUpdate);
            logger.info("Finished exporting workflows and family ID-s for instance with id " + instanceId);
        }
        catch (IOException e) {
            logger.error("Error exporting workflows and family ids for instanceId " + instanceId, e);
        }
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

    public void checkWorkflowNamesAndExport(RestHighLevelClient client, List<String> workFlowColumnNames, List<ParticipantDataDto> allParticipantData,
                                                   int instanceId, AtomicBoolean clearBeforeUpdate) {
        DDPInstance ddpInstance = DDPInstance.getDDPInstanceById(instanceId);
        Optional<String> maybeEsParticipantIndex =
                new DDPInstanceDao().getEsParticipantIndexByInstanceId(instanceId);
        for (ParticipantDataDto participantData: allParticipantData) {
            String data = participantData.getData().orElse(null);
            if (data == null) {
                continue;
            }
            String ddpParticipantId = participantData.getDdpParticipantId().orElse("");
            String finalDdpParticipantId = ddpParticipantId;
            List<ParticipantDataDto> participantDataFamily = allParticipantData.stream()
                    .filter(participantDataDto -> participantDataDto.getDdpParticipantId().orElse("").equals(finalDdpParticipantId))
                    .collect(Collectors.toList());
            if (!ParticipantUtil.isGuid(ddpParticipantId)) {
                ddpParticipantId = getGuidIfWeHaveAltpid(ddpParticipantId, maybeEsParticipantIndex);
            }
            if ("".equals(ddpParticipantId)) {
                continue;
            }
            Map<String, String> dataMap = gson.fromJson(data, Map.class);
            exportWorkflows(client, workFlowColumnNames, ddpInstance, participantData, ddpParticipantId, participantDataFamily, dataMap, clearBeforeUpdate);
            if (dataMap.containsKey(FamilyMemberConstants.FAMILY_ID)) {
                ElasticSearchUtil.writeDsmRecord(client, ddpInstance, null,
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

    public void exportWorkflows(RestHighLevelClient client, List<String> workFlowColumnNames, DDPInstance ddpInstance, ParticipantDataDto participantData,
                                       String ddpParticipantId, List<ParticipantDataDto> participantDataFamily, Map<String, String> dataMap,
                                       AtomicBoolean clearBeforeUpdate) {
        if (participantData.getFieldTypeId().orElse("").equals(RGP_PARTICIPANTS)) {
            String collaboratorParticipantId = dataMap.get(FamilyMemberConstants.COLLABORATOR_PARTICIPANT_ID);
            if (!ParticipantUtil.matchesApplicantEmail(collaboratorParticipantId, participantDataFamily)) {
                ElasticSearchUtil.removeWorkflowIfNoDataOrWrongSubject(client, ddpParticipantId, ddpInstance, collaboratorParticipantId);
            } else {
                //is matching applicant email => write into ES
                WorkflowForES.StudySpecificData studySpecificData = new WorkflowForES.StudySpecificData(
                        collaboratorParticipantId,
                        dataMap.get(FamilyMemberConstants.FIRSTNAME),
                        dataMap.get(FamilyMemberConstants.LASTNAME)
                );
                dataMap.entrySet().stream().filter(entry -> workFlowColumnNames.contains(entry.getKey()))
                        .forEach(entry -> {
                            ElasticSearchUtil.writeWorkflow(client, WorkflowForES.createInstanceWithStudySpecificData(ddpInstance, ddpParticipantId,
                                    entry.getKey(), entry.getValue(), studySpecificData), clearBeforeUpdate.get());
                            clearBeforeUpdate.set(false);
                        });
            }
        } else {
            dataMap.entrySet().stream().filter(entry -> workFlowColumnNames.contains(entry.getKey()))
                    .forEach(entry -> {
                        ElasticSearchUtil.writeWorkflow(client, WorkflowForES.createInstance(ddpInstance, ddpParticipantId,
                                entry.getKey(), entry.getValue()), clearBeforeUpdate.get());
                        clearBeforeUpdate.set(false);
                    });
        }
    }
}
