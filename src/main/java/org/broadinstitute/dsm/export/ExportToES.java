package org.broadinstitute.dsm.export;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.broadinstitute.dsm.db.DDPInstance;
import org.broadinstitute.dsm.db.dao.ddp.instance.DDPInstanceDao;
import org.broadinstitute.dsm.db.dao.ddp.kitrequest.KitRequestDao;
import org.broadinstitute.dsm.db.dao.ddp.medical.records.ESMedicalRecordsDao;
import org.broadinstitute.dsm.db.dao.ddp.tissue.ESTissueRecordsDao;
import org.broadinstitute.dsm.db.dao.fieldsettings.FieldSettingsDao;
import org.broadinstitute.dsm.db.dao.participant.data.ParticipantDataDao;
import org.broadinstitute.dsm.db.dto.ddp.kitrequest.ESSamplesDto;
import org.broadinstitute.dsm.db.dto.fieldsettings.FieldSettingsDto;
import org.broadinstitute.dsm.db.dto.medical.records.ESMedicalRecordsDto;
import org.broadinstitute.dsm.db.dto.participant.data.ParticipantDataDto;
import org.broadinstitute.dsm.db.dto.ddp.tissue.ESTissueRecordsDto;
import org.broadinstitute.dsm.model.Value;
import org.broadinstitute.dsm.statics.ESObjectConstants;
import org.broadinstitute.dsm.util.ElasticSearchUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ExportToES {

    private static final Logger logger = LoggerFactory.getLogger(ExportToES.class);
    private static final Gson gson = new Gson();
    private static final ParticipantDataDao participantDataDao = new ParticipantDataDao();
    private static final DDPInstanceDao ddpInstanceDao = new DDPInstanceDao();
    private static final ESMedicalRecordsDao esMedicalRecordsDao = new ESMedicalRecordsDao();
    private static final ESTissueRecordsDao esTissueRecordsDao = new ESTissueRecordsDao();
    private static final KitRequestDao kitRequestDao = new KitRequestDao();
    private static final ObjectMapper oMapper = new ObjectMapper();
    public static final String MEMBER_TYPE = "MEMBER_TYPE";
    public static final String SELF = "SELF";
    public static final String FAMILY_ID = "FAMILY_ID";

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
        for (ParticipantDataDto participantData: allParticipantData) {
            String data = participantData.getData();
            if (data != null) {
                JsonObject dataJsonObject = gson.fromJson(data, JsonObject.class);
                for (Map.Entry<String, JsonElement> entry: dataJsonObject.entrySet()) {
                    if (workFlowColumnNames.contains(entry.getKey())) {
                        ElasticSearchUtil.writeWorkflow(ddpInstance, participantData.getDdpParticipantId(),
                                entry.getKey(), entry.getValue().getAsString());
                    }
                }
                if (dataJsonObject.has(MEMBER_TYPE) && dataJsonObject.get(MEMBER_TYPE).getAsString().equals(SELF)
                    && dataJsonObject.has(FAMILY_ID)) {
                    ElasticSearchUtil.writeDsmRecord(ddpInstance, null,
                            participantData.getDdpParticipantId(), ESObjectConstants.FAMILY_ID, dataJsonObject.get(FAMILY_ID).getAsString(), null);
                }
            }
        }
    }

    private static List<String> findWorkFlowColumnNames(int instanceId) {
        FieldSettingsDao fieldSettingsDao = new FieldSettingsDao();
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
