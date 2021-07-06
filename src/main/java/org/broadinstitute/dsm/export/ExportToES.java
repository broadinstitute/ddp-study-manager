package org.broadinstitute.dsm.export;

import com.google.gson.Gson;
import org.broadinstitute.dsm.db.dao.ddp.instance.DDPInstanceDao;

import java.util.concurrent.atomic.AtomicBoolean;

public class ExportToES {

    private static final Gson gson = new Gson();
    private static final DDPInstanceDao ddpInstanceDao = new DDPInstanceDao();
    private WorkflowAndFamilyIdExporter workflowAndFamilyIdExporter;
    private TissueRecordExporter tissueRecordExporter;
    private MedicalRecordExporter medicalRecordExporter;
    private SampleExporter sampleExporter;

    public ExportToES() {
        this.workflowAndFamilyIdExporter = new WorkflowAndFamilyIdExporter();
        this.tissueRecordExporter = new TissueRecordExporter();
        this.medicalRecordExporter = new MedicalRecordExporter();
        this.sampleExporter = new SampleExporter();
    }

    public void exportObjectsToES(String data, AtomicBoolean clearBeforeUpdate) {
        ExportPayload payload = gson.fromJson(data, ExportPayload.class);
        int instanceId = ddpInstanceDao.getDDPInstanceIdByGuid(payload.getStudy());

        workflowAndFamilyIdExporter.export(instanceId, clearBeforeUpdate);

        medicalRecordExporter.export(instanceId);

        tissueRecordExporter.export(instanceId);

        sampleExporter.export(instanceId);
    }

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
}
