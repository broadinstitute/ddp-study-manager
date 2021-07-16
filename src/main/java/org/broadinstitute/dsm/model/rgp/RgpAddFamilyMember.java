package org.broadinstitute.dsm.model.rgp;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.broadinstitute.dsm.db.DDPInstance;
import org.broadinstitute.dsm.db.dao.fieldsettings.FieldSettingsDao;
import org.broadinstitute.dsm.db.dto.fieldsettings.FieldSettingsDto;
import org.broadinstitute.dsm.export.WorkflowForES;
import org.broadinstitute.dsm.model.familymember.AddFamilyMember;
import org.broadinstitute.dsm.model.fieldsettings.FieldSettings;
import org.broadinstitute.dsm.model.participant.data.AddFamilyMemberPayload;
import org.broadinstitute.dsm.util.ElasticSearchUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RgpAddFamilyMember extends AddFamilyMember {

    private static final Logger logger = LoggerFactory.getLogger(RgpAddFamilyMember.class);

    public RgpAddFamilyMember(AddFamilyMemberPayload addFamilyMemberPayload) {
        super(addFamilyMemberPayload);
    }

    @Override
    public void exportDataToEs() {
        boolean isCopyProband = addFamilyMemberPayload.getCopyProbandInfo().orElse(Boolean.FALSE);
        if (!isCopyProband) return;
        exportProbandDataForFamilyMemberToEs();
    }

    private void exportProbandDataForFamilyMemberToEs() {
        if(!participantData.hasFamilyMemberApplicantEmail()) return;
        List<FieldSettingsDto> fieldSettingsByInstanceIdAndColumns =
                getFieldSettingsDtosByInstanceIdAndColumns();
        FieldSettings fieldSettings = new FieldSettings();
        logger.info("Starting exporting copied proband data to family member into ES");
        fieldSettingsByInstanceIdAndColumns.forEach(fieldSettingsDto -> {
            if (!fieldSettings.isElasticExportWorkflowType(fieldSettingsDto)) return;
            WorkflowForES instanceWithStudySpecificData =
                    WorkflowForES.createInstanceWithStudySpecificData(DDPInstance.getDDPInstance(studyGuid), addFamilyMemberPayload.getParticipantId().get(),
                            fieldSettingsDto.getColumnName(),
                            participantData.getData().get(fieldSettingsDto.getColumnName()),
                            new WorkflowForES.StudySpecificData(
                                    addFamilyMemberPayload.getData().get().getCollaboratorParticipantId(),
                                    addFamilyMemberPayload.getData().get().getFirstName(),
                                    addFamilyMemberPayload.getData().get().getLastName()));
            ElasticSearchUtil.writeWorkflow(instanceWithStudySpecificData, false);
        });
    }

    private List<FieldSettingsDto> getFieldSettingsDtosByInstanceIdAndColumns() {
        FieldSettingsDao fieldSettingsDao = FieldSettingsDao.of();
        ArrayList<String> columns = new ArrayList<>(Objects.requireNonNull(participantData.getData()).keySet());
        return fieldSettingsDao.getFieldSettingsByInstanceIdAndColumns(
                ddpInstanceId,
                columns
        );
    }

}
