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
import org.broadinstitute.dsm.pubsub.ElasticExportSubscription.ExportPayload;
import org.broadinstitute.dsm.statics.ESObjectConstants;
import org.broadinstitute.dsm.util.ElasticSearchUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ExportToES {

    private static final Gson gson = new Gson();
    private static final ParticipantDataDao participantDataDao = new ParticipantDataDao();
    private static final DDPInstanceDao ddpInstanceDao = new DDPInstanceDao();
    private static final ESMedicalRecordsDao esMedicalRecordsDao = new ESMedicalRecordsDao();
    private static final ESTissueRecordsDao esTissueRecordsDao = new ESTissueRecordsDao();
    private static final KitRequestDao kitRequestDao = new KitRequestDao();
    private static final ObjectMapper oMapper = new ObjectMapper();

    public static void exportObjectsToES(ExportPayload payload) {
        int instanceId = ddpInstanceDao.getDDPInstanceIdByGuid(payload.getStudy());
        exportWorkflows(instanceId);
        exportMedicalRecords(instanceId);
        exportTissueRecords(instanceId);
        exportSamples(instanceId);
    }

    public static void exportWorkflows(int instanceId) {
        List<String> workFlowColumnNames = findWorkFlowColumnNames(instanceId);
        List<ParticipantDataDto> allParticipantData = participantDataDao.getParticipantDataByInstanceid(instanceId);
        checkWorkflowNamesAndExport(workFlowColumnNames, allParticipantData, instanceId);
    }

    public static void exportSamples(int instanceId) {
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
    }

    public static void exportMedicalRecords(int instanceId) {
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
    }

    private static void exportTissueRecords(int instanceId) {
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
    }

    public static void checkWorkflowNamesAndExport(List<String> workFlowColumnNames, List<ParticipantDataDto> allParticipantData, int instanceId) {
        for (ParticipantDataDto participantData: allParticipantData) {
            String data = participantData.getData();
            DDPInstance ddpInstance = DDPInstance.getDDPInstanceById(instanceId);
            if (data != null) {
                JsonObject dataJsonObject = gson.fromJson(data, JsonObject.class);
                for (Map.Entry<String, JsonElement> entry: dataJsonObject.entrySet()) {
                    if (workFlowColumnNames.contains(entry.getKey())) {
                        ElasticSearchUtil.writeWorkflow(ddpInstance, participantData.getDdpParticipantId(), entry.getKey(), entry.getValue().getAsString());
                    }
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
