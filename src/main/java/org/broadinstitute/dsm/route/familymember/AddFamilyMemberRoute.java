package org.broadinstitute.dsm.route.familymember;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import com.google.gson.Gson;
import org.broadinstitute.ddp.handlers.util.Result;
import org.broadinstitute.dsm.db.DDPInstance;
import org.broadinstitute.dsm.db.User;
import org.broadinstitute.dsm.db.dao.ddp.instance.DDPInstanceDao;
import org.broadinstitute.dsm.db.dao.fieldsettings.FieldSettingsDao;
import org.broadinstitute.dsm.db.dao.ddp.participant.ParticipantDataDao;
import org.broadinstitute.dsm.db.dto.fieldsettings.FieldSettingsDto;
import org.broadinstitute.dsm.export.WorkflowForES;
import org.broadinstitute.dsm.model.fieldsettings.FieldSettings;
import org.broadinstitute.dsm.model.participant.data.ParticipantData;
import org.broadinstitute.dsm.model.participant.data.AddFamilyMemberPayload;
import org.broadinstitute.dsm.model.participant.data.FamilyMemberDetails;
import org.broadinstitute.dsm.security.RequestHandler;
import org.broadinstitute.dsm.statics.DBConstants;
import org.broadinstitute.dsm.util.ElasticSearchUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;

public class AddFamilyMemberRoute extends RequestHandler {

    private static final Logger logger = LoggerFactory.getLogger(AddFamilyMemberRoute.class);

    @Override
    protected Object processRequest(Request request, Response response, String userId) throws Exception {
        Gson gson = new Gson();
        AddFamilyMemberPayload addFamilyMemberPayload = gson.fromJson(request.body(), AddFamilyMemberPayload.class);

        String participantId = addFamilyMemberPayload.getParticipantId()
                .orElseThrow(() -> new NoSuchElementException("Participant Guid is not provided"));

        String realm =
                addFamilyMemberPayload.getRealm().orElseThrow(() -> new NoSuchElementException("Realm is not provided"));
        boolean isRealmAbleOfAddFamilyMember = DDPInstanceDao.getRole(realm, DBConstants.ADD_FAMILY_MEMBER);
        if (!isRealmAbleOfAddFamilyMember) {
            response.status(400);
            logger.warn("Study : " + realm + " is not setup to add family member");
            return new Result(400, "Study is not setup to add family member");
        }
        DDPInstance ddpInstance = DDPInstance.getDDPInstance(realm);
        String ddpInstanceId = ddpInstance.getDdpInstanceId();

        Optional<FamilyMemberDetails> maybeFamilyMemberData = addFamilyMemberPayload.getData();
        if (maybeFamilyMemberData.isEmpty() || maybeFamilyMemberData.orElseGet(FamilyMemberDetails::new).isFamilyMemberFieldsEmpty()) {
            response.status(400);
            logger.warn("Family member information for participant : " + participantId + " is not provided");
            return new Result(400, "Family member information is not provided");
        }

        Integer uId =
                addFamilyMemberPayload.getUserId().orElseThrow(() -> new NoSuchElementException("User id is not provided"));
        if (Integer.parseInt(userId) != uId) {
            throw new RuntimeException("User id was not equal. User id in token " + userId + " user id in request " + uId);
        }

        ParticipantDataDao participantDataDao = new ParticipantDataDao();
        ParticipantData participantDataObject = new ParticipantData(participantDataDao);
        participantDataObject.setDdpParticipantId(participantId);
        participantDataObject.setDdpInstanceId(Integer.parseInt(ddpInstanceId));
        participantDataObject.setFieldTypeId(realm.toUpperCase() + ParticipantData.FIELD_TYPE);
        participantDataObject.setFamilyMemberData(addFamilyMemberPayload);
        participantDataObject.copyProbandData(addFamilyMemberPayload);
        participantDataObject.addDefaultOptionsValueToData(getDefaultOptions(Integer.parseInt(ddpInstanceId)));
        exportDataToEs(addFamilyMemberPayload, ddpInstance, participantDataObject);
        long createdParticipantDataId = participantDataObject.insertParticipantData(User.getUser(uId).getEmail());
        logger.info("Family member for participant " + participantId + " successfully created");
        return ParticipantData.parseDto(participantDataDao.get(createdParticipantDataId).orElseThrow(() -> {
            throw new NoSuchElementException("Could not find participant data with id: " + createdParticipantDataId);
        }));
    }

    private void exportDataToEs(AddFamilyMemberPayload addFamilyMemberPayload, DDPInstance ddpInstance,
                                ParticipantData participantDataObject) {
        boolean isCopyProband = addFamilyMemberPayload.getCopyProbandInfo().orElse(Boolean.FALSE);
        if (isCopyProband) {
            exportProbandDataForFamilyMemberToEs(addFamilyMemberPayload, ddpInstance, participantDataObject);
        } else {
            exportDefaultWorkflowsForFamilyMemberToES(addFamilyMemberPayload.getParticipantId().get(), ddpInstance, addFamilyMemberPayload.getData().get());
        }
    }

    private void exportProbandDataForFamilyMemberToEs(AddFamilyMemberPayload addFamilyMemberPayload, DDPInstance ddpInstance,
                                                      ParticipantData participantDataObject) {
        List<FieldSettingsDto> fieldSettingsByInstanceIdAndColumns =
                getFieldSettingsDtosByInstanceIdAndColumns(
                        Integer.parseInt(ddpInstance.getDdpInstanceId()),
                        new ArrayList<>(participantDataObject.getData().keySet())
                );
        FieldSettings fieldSettings = new FieldSettings();
        logger.info("Starting exporting copied proband data to family member into ES");
        fieldSettingsByInstanceIdAndColumns.forEach(fieldSettingsDto -> {
            if (!fieldSettings.isElasticExportWorkflowType(fieldSettingsDto)) return;
            WorkflowForES instanceWithStudySpecificData =
                    WorkflowForES.createInstanceWithStudySpecificData(ddpInstance, addFamilyMemberPayload.getParticipantId().get(),
                            fieldSettingsDto.getColumnName(),
                            participantDataObject.getData().get(fieldSettingsDto.getColumnName()),
                            new WorkflowForES.StudySpecificData(
                                    addFamilyMemberPayload.getData().get().getCollaboratorParticipantId(),
                                    addFamilyMemberPayload.getData().get().getFirstName(),
                                    addFamilyMemberPayload.getData().get().getLastName()));
            ElasticSearchUtil.writeWorkflow(instanceWithStudySpecificData, false);
        });
    }

    private List<FieldSettingsDto> getFieldSettingsDtosByInstanceIdAndColumns(int instanceId, List<String> columns) {
        FieldSettingsDao fieldSettingsDao = FieldSettingsDao.of();
        return fieldSettingsDao.getFieldSettingsByInstanceIdAndColumns(
                instanceId,
                columns
        );
    }

    private void exportDefaultWorkflowsForFamilyMemberToES(String participantId, DDPInstance ddpInstance,
                           FamilyMemberDetails maybeFamilyMemberData) {
        logger.info("Exporting workflow for family member of participant: " + participantId + " to ES");
        getDefaultOptionsByElasticWorkflow(Integer.parseInt(ddpInstance.getDdpInstanceId())).forEach((col, val) -> {
            WorkflowForES instanceWithStudySpecificData =
                    WorkflowForES.createInstanceWithStudySpecificData(ddpInstance, participantId, col, val,
                            new WorkflowForES.StudySpecificData(
                                    maybeFamilyMemberData.getCollaboratorParticipantId(),
                                    maybeFamilyMemberData.getFirstName(),
                                    maybeFamilyMemberData.getLastName()));
            ElasticSearchUtil.writeWorkflow(instanceWithStudySpecificData, false);
        });
    }

    private Map<String, String> getDefaultOptions(int ddpInstanceId) {
        FieldSettingsDao fieldSettingsDao = FieldSettingsDao.of();
        FieldSettings fieldSettings = new FieldSettings();
        return fieldSettings.getColumnsWithDefaultOptions(fieldSettingsDao.getFieldSettingsByOptionAndInstanceId(ddpInstanceId));
    }

    private Map<String, String> getDefaultOptionsByElasticWorkflow(int instanceId) {
        FieldSettingsDao fieldSettingsDao = FieldSettingsDao.of();
        FieldSettings fieldSettings = new FieldSettings();
        List<FieldSettingsDto> fieldSettingsByInstanceId = fieldSettingsDao.getFieldSettingsByInstanceId(instanceId);
        return fieldSettings.getColumnsWithDefaultOptionsFilteredByElasticExportWorkflow(fieldSettingsByInstanceId);
    }



}
