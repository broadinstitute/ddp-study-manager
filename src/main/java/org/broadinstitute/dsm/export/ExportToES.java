package org.broadinstitute.dsm.export;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import org.broadinstitute.dsm.db.DDPInstance;
import org.broadinstitute.dsm.db.dao.ddp.instance.DDPInstanceDao;
import org.broadinstitute.dsm.db.dao.ddp.kitrequest.KitRequestDao;
import org.broadinstitute.dsm.db.dao.ddp.medical.records.ESMedicalRecordsDao;
import org.broadinstitute.dsm.db.dao.ddp.participant.ParticipantDataDao;
import org.broadinstitute.dsm.db.dao.ddp.tissue.ESTissueRecordsDao;
import org.broadinstitute.dsm.db.dao.fieldsettings.FieldSettingsDao;
import org.broadinstitute.dsm.db.dto.ddp.kitrequest.ESSamplesDto;
import org.broadinstitute.dsm.db.dto.ddp.participant.ParticipantDataDto;
import org.broadinstitute.dsm.db.dto.ddp.tissue.ESTissueRecordsDto;
import org.broadinstitute.dsm.db.dto.fieldsettings.FieldSettingsDto;
import org.broadinstitute.dsm.db.dto.medical.records.ESMedicalRecordsDto;
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
import java.util.stream.Collectors;

public class ExportToES {

    private static final Logger logger = LoggerFactory.getLogger(ExportToES.class);
    private static final Gson gson = new Gson();
    private static final ParticipantDataDao participantDataDao = new ParticipantDataDao();
    private static final DDPInstanceDao ddpInstanceDao = new DDPInstanceDao();
    private static final ESMedicalRecordsDao esMedicalRecordsDao = new ESMedicalRecordsDao();
    private static final ESTissueRecordsDao esTissueRecordsDao = new ESTissueRecordsDao();
    private static final KitRequestDao kitRequestDao = new KitRequestDao();
    private static final ObjectMapper oMapper = new ObjectMapper();
    public static final String FAMILY_ID = "FAMILY_ID";
    public static final String RGP_PARTICIPANTS = "RGP_PARTICIPANTS";

    public static class ExportPayload {
        private String index;
        private String study;

        public String getIndex() {
            return index;
        }

        public String getStudy() {
            return study;
        }
    }

    public static void exportObjectsToES(String data) {
        ExportPayload payload = gson.fromJson(data, ExportPayload.class);
        int instanceId = ddpInstanceDao.getDDPInstanceIdByGuid(payload.getStudy());

        exportWorkflowsAndFamilyIds(instanceId);

        exportMedicalRecords(instanceId);

        exportTissueRecords(instanceId);

        exportSamples(instanceId);
    }

    public static void exportWorkflowsAndFamilyIds(int instanceId) {
        logger.info("Started exporting workflows and family ID-s for instance with id " + instanceId);
        List<String> workFlowColumnNames = findWorkFlowColumnNames(instanceId);
        List<ParticipantDataDto> allParticipantData = participantDataDao.getParticipantDataByInstanceid(instanceId);
        checkWorkflowNamesAndExport(workFlowColumnNames, allParticipantData, instanceId);
        logger.info("Finished exporting workflows and family ID-s for instance with id " + instanceId);
    }

    public static void exportSamples(int instanceId) {
        logger.info("Started exporting samples for instance with id " + instanceId);
        List<ESSamplesDto> esSamples = kitRequestDao.getESSamplesByInstanceId(instanceId);
        DDPInstance ddpInstance = DDPInstance.getDDPInstanceById(instanceId);
        if (ddpInstance != null) {
            for (ESSamplesDto sample: esSamples) {
                Map<String, Object> map = oMapper.convertValue(sample, Map.class);
                if (sample.getKitRequestId() != null && sample.getDdpParticipantId() != null) {
                    ElasticSearchUtil.writeSample(ddpInstance, sample.getKitRequestId(), sample.getDdpParticipantId(),
                            ESObjectConstants.SAMPLES, ESObjectConstants.KIT_REQUEST_ID, map);
                }
            }
        }
        logger.info("Finished exporting samples for instance with id " + instanceId);
    }

    public static void exportMedicalRecords(int instanceId) {
        logger.info("Started exporting medical records for instance with id " + instanceId);
        List<ESMedicalRecordsDto> esMedicalRecords = esMedicalRecordsDao.getESMedicalRecordsByInstanceId(instanceId);
        DDPInstance ddpInstance = DDPInstance.getDDPInstanceById(instanceId);
        if (ddpInstance != null) {
            for (ESMedicalRecordsDto medicalRecord : esMedicalRecords) {
                Map<String, Object> map = oMapper.convertValue(medicalRecord, Map.class);
                if (medicalRecord.getMedicalRecordId() != null && medicalRecord.getDdpParticipantId() != null) {
                    ElasticSearchUtil.writeDsmRecord(ddpInstance, medicalRecord.getMedicalRecordId(), medicalRecord.getDdpParticipantId(),
                            ESObjectConstants.MEDICAL_RECORDS, ESObjectConstants.MEDICAL_RECORDS_ID, map);
                }
            }
        }
        logger.info("Finished exporting medical records for instance with id " + instanceId);
    }

    private static void exportTissueRecords(int instanceId) {
        logger.info("Started exporting tissue records for instance with id " + instanceId);
        List<ESTissueRecordsDto> esTissueRecords = esTissueRecordsDao.getESTissueRecordsByInstanceId(instanceId);
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

    public static void checkWorkflowNamesAndExport(List<String> workFlowColumnNames, List<ParticipantDataDto> allParticipantData, int instanceId) {
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
            exportWorkflows(workFlowColumnNames, ddpInstance, participantData, ddpParticipantId, participantDataFamily, dataMap);
            if (dataMap.containsKey(FamilyMemberConstants.FAMILY_ID)) {
                ElasticSearchUtil.writeDsmRecord(ddpInstance, null,
                        ddpParticipantId, ESObjectConstants.FAMILY_ID, dataMap.get(FAMILY_ID), null);
            }
        }
    }

    public static void exportWorkflows(List<String> workFlowColumnNames, DDPInstance ddpInstance, ParticipantDataDto participantData,
                                       String ddpParticipantId, List<ParticipantDataDto> participantDataFamily, Map<String, String> dataMap) {
        if (participantData.getFieldTypeId().equals(RGP_PARTICIPANTS)) {
            if (!ParticipantUtil.checkProbandEmail(dataMap.get(FamilyMemberConstants.COLLABORATOR_PARTICIPANT_ID), participantDataFamily)) {
                return;
            }
            WorkflowForES.StudySpecificData studySpecificData = new WorkflowForES.StudySpecificData(
                    dataMap.get(FamilyMemberConstants.COLLABORATOR_PARTICIPANT_ID),
                    dataMap.get(FamilyMemberConstants.FIRSTNAME),
                    dataMap.get(FamilyMemberConstants.LASTNAME)
            );
            dataMap.entrySet().stream().filter(entry -> workFlowColumnNames.contains(entry.getKey()))
                    .forEach(entry -> ElasticSearchUtil.writeWorkflow(WorkflowForES.createInstanceWithStudySpecificData(ddpInstance, ddpParticipantId,
                            entry.getKey(), entry.getValue(), studySpecificData)));
        } else {
            dataMap.entrySet().stream().filter(entry -> workFlowColumnNames.contains(entry.getKey()))
                    .forEach(entry -> ElasticSearchUtil.writeWorkflow(WorkflowForES.createInstance(ddpInstance, ddpParticipantId,
                            entry.getKey(), entry.getValue())));
        }
    }

    public static String getGuidIfWeHaveAltpid(String ddpParticipantId, Optional<String> maybeEsParticipantIndex) {
        if (maybeEsParticipantIndex.isPresent()) {
            ElasticSearch participantESDataByAltpid =
                    ElasticSearchUtil.getParticipantESDataByAltpid(maybeEsParticipantIndex.get(), ddpParticipantId);
            ddpParticipantId = participantESDataByAltpid.getProfile().map(ESProfile::getParticipantGuid).orElse("");
        } else {
            logger.error("Wrong instance ID");
        }
        return ddpParticipantId;
    }

    private static List<String> findWorkFlowColumnNames(int instanceId) {
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
}
