package org.broadinstitute.dsm.route;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.ddp.handlers.util.Result;
import org.broadinstitute.dsm.analytics.GoogleAnalyticsMetrics;
import org.broadinstitute.dsm.analytics.GoogleAnalyticsMetricsTracker;
import org.broadinstitute.dsm.db.*;
import org.broadinstitute.dsm.db.dao.ddp.participant.ParticipantDao;
import org.broadinstitute.dsm.db.dao.ddp.participant.ParticipantDataDao;
import org.broadinstitute.dsm.db.dao.ddp.participant.ParticipantRecordDao;
import org.broadinstitute.dsm.db.dao.queue.EventDao;
import org.broadinstitute.dsm.db.dao.settings.EventTypeDao;
import org.broadinstitute.dsm.db.dao.user.UserDao;
import org.broadinstitute.dsm.db.dto.ddp.participant.ParticipantDto;
import org.broadinstitute.dsm.db.dto.ddp.participant.ParticipantRecordDto;
import org.broadinstitute.dsm.db.dto.settings.EventTypeDto;
import org.broadinstitute.dsm.db.dto.user.UserDto;
import org.broadinstitute.dsm.db.structure.DBElement;
import org.broadinstitute.dsm.exception.DuplicateException;
import org.broadinstitute.dsm.export.WorkflowForES;
import org.broadinstitute.dsm.model.AbstractionWrapper;
import org.broadinstitute.dsm.model.NameValue;
import org.broadinstitute.dsm.model.Patch;
import org.broadinstitute.dsm.model.Value;
import org.broadinstitute.dsm.model.elasticsearch.ESProfile;
import org.broadinstitute.dsm.model.participant.data.FamilyMemberConstants;
import org.broadinstitute.dsm.model.settings.field.FieldSettings;
import org.broadinstitute.dsm.security.RequestHandler;
import org.broadinstitute.dsm.statics.DBConstants;
import org.broadinstitute.dsm.statics.ESObjectConstants;
import org.broadinstitute.dsm.statics.UserErrorMessages;
import org.broadinstitute.dsm.util.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.broadinstitute.ddp.db.TransactionWrapper.inTransaction;

//Class needs to be refactored as soon as possible!!!
public class PatchRoute extends RequestHandler {

    private static final Logger logger = LoggerFactory.getLogger(PatchRoute.class);
    private static final ParticipantDataDao participantDataDao = new ParticipantDataDao();
    private static final EventDao eventDao = new EventDao();
    private static final EventTypeDao eventTypeDao = new EventTypeDao();
    private static final Gson gson = new GsonBuilder().serializeNulls().create();

    private static final String PARTICIPANT_ID = "participantId";
    private static final String PRIMARY_KEY_ID = "primaryKeyId";
    private static final String NAME_VALUE = "NameValue";
    private static final String STATUS = "status";

    private NotificationUtil notificationUtil;
    private PatchUtil patchUtil;

    public PatchRoute(@NonNull NotificationUtil notificationUtil, @NonNull PatchUtil patchUtil) {
        this.notificationUtil = notificationUtil;
        this.patchUtil = patchUtil;
    }

    @Override
    public Object processRequest(Request request, Response response, String userId) throws Exception {
        if (patchUtil.getColumnNameMap() == null) {
            return new RuntimeException("ColumnNameMap is null!");
        }
        String userIdRequest = UserUtil.getUserId(request);
        if (UserUtil.checkUserAccess(null, userId, DBConstants.MR_VIEW, userIdRequest) || UserUtil.checkUserAccess(null, userId, DBConstants.MR_ABSTRACTER, userIdRequest)
                || UserUtil.checkUserAccess(null, userId, DBConstants.MR_VIEW, userIdRequest) || UserUtil.checkUserAccess(null, userId, DBConstants.PT_LIST_VIEW, userIdRequest)) {
            try {
                String requestBody = request.body();
                Patch patch = gson.fromJson(requestBody, Patch.class);
                if (StringUtils.isNotBlank(patch.getId())) {
                    //multiple values are changing
                    DDPInstance ddpInstance = DDPInstance.getDDPInstance(patch.getRealm());
                    if (patch.getNameValues() != null && !patch.getNameValues().isEmpty()) {
                        List<NameValue> nameValues = new ArrayList<>();
                        ESProfile profile = ElasticSearchUtil.getParticipantProfileByGuidOrAltPid(ddpInstance.getParticipantIndexES(), patch.getDdpParticipantId())
                                .orElse(null);
                        if (profile == null) {
                            logger.error("Unable to find ES profile for participant with guid/altpid: {}, continuing w/ patch", patch.getParentId());
                        }
                        for (NameValue nameValue : patch.getNameValues()) {
                            DBElement dbElement = patchUtil.getColumnNameMap().get(nameValue.getName());
                            if (dbElement != null) {
                                if (!Patch.patch(patch.getId(), patch.getUser(), nameValue, dbElement)) {
                                    return new RuntimeException("An error occurred while attempting to patch ");
                                }
                                if (nameValue.getName().indexOf("question") > -1) {
                                    UserDto userDto = new UserDao().getUserByEmail(patch.getUser()).orElseThrow();
                                    JSONObject jsonObject = new JSONObject(nameValue.getValue().toString());
                                    JSONArray questionArray = new JSONArray(jsonObject.get("questions").toString());
                                    boolean writeBack = false;
                                    for (int i = 0; i < questionArray.length(); i++) {
                                        JSONObject question = questionArray.getJSONObject(i);
                                        if (question.optString(STATUS) != null && question.optString(STATUS).equals("sent")) {
                                            if (question.optString("email") != null && question.optString("question") != null) {
                                                notificationUtil.sentAbstractionExpertQuestion(userDto.getEmail().orElse(""), userDto.getName().orElse(""), question.optString("email"),
                                                        patch.getFieldName(), question.optString("question"), notificationUtil.getTemplate("DSM_ABSTRACTION_EXPERT_QUESTION"));
                                            }
                                            question.put(STATUS, "done");
                                            writeBack = true;
                                        }
                                    }
                                    if (writeBack) {
                                        jsonObject.put("questions", questionArray);
                                        String str = jsonObject.toString();
                                        nameValue.setValue(str);
                                        if (!Patch.patch(patch.getId(), patch.getUser(), nameValue, dbElement)) {
                                            return new RuntimeException("An error occurred while attempting to patch ");
                                        }
                                        nameValues.add(nameValue);
                                    }
                                }
                                controlWorkflowByEmail(patch, nameValue, ddpInstance, profile);
                                if (patch.getActions() != null) {
                                    for (Value action : patch.getActions()) {
                                        if (ESObjectConstants.ELASTIC_EXPORT_WORKFLOWS.equals(action.getType()) && profile != null) {
                                            writeESWorkflow(patch, nameValue, action, ddpInstance, profile.getParticipantGuid());
                                        }
                                        else if (EventTypeDao.EVENT.equals(action.getType())) {
                                            triggerParticipantEvent(ddpInstance, patch, action);
                                        }
                                    }
                                }
                            }
                            else {
                                GoogleAnalyticsMetricsTracker.getInstance().sendAnalyticsMetrics(ddpInstance.getName(), GoogleAnalyticsMetrics.EVENT_CATEGORY_PATCH_DATA,
                                        GoogleAnalyticsMetrics.EVENT_ACTION_PATCH_DATA, GoogleAnalyticsMetrics.EVENT_LABEL_PATCH_DATA, null, 1);
                                throw new RuntimeException("DBElement not found in ColumnNameMap: " + nameValue.getName());
                            }
                        }
                        GoogleAnalyticsMetricsTracker.getInstance().sendAnalyticsMetrics(ddpInstance.getName(), GoogleAnalyticsMetrics.EVENT_CATEGORY_PATCH_DATA,
                                GoogleAnalyticsMetrics.EVENT_ACTION_PATCH_DATA, GoogleAnalyticsMetrics.EVENT_LABEL_PATCH_DATA, null, 1);
                        return new Result(200, gson.toJson(nameValues));
                    }
                    else {
                        // mr changes
                        DBElement dbElement = patchUtil.getColumnNameMap().get(patch.getNameValue().getName());
                        if (dbElement != null) {
                            if (Patch.patch(patch.getId(), patch.getUser(), patch.getNameValue(), dbElement)) {
                                List<NameValue> nameValues = setWorkflowRelatedFields(patch);
                                writeDSMRecordsToES(patch, ddpInstance);
                                //return nameValues with nulls
                                GoogleAnalyticsMetricsTracker.getInstance().sendAnalyticsMetrics(ddpInstance.getName(), GoogleAnalyticsMetrics.EVENT_CATEGORY_PATCH_DATA,
                                        GoogleAnalyticsMetrics.EVENT_ACTION_PATCH_DATA, GoogleAnalyticsMetrics.EVENT_LABEL_PATCH_DATA, null, 1);
                                return new Result(200, gson.toJson(nameValues));
                            }
                        }
                        else {
                            GoogleAnalyticsMetricsTracker.getInstance().sendAnalyticsMetrics(ddpInstance.getName(), GoogleAnalyticsMetrics.EVENT_CATEGORY_PATCH_DATA,
                                    GoogleAnalyticsMetrics.EVENT_ACTION_PATCH_DATA, GoogleAnalyticsMetrics.EVENT_LABEL_PATCH_DATA, null, 1);
                            throw new RuntimeException("DBElement not found in ColumnNameMap: " + patch.getNameValue().getName());
                        }
                    }
                }
                else if (StringUtils.isNotBlank(patch.getParent()) && StringUtils.isNotBlank(patch.getParentId())) {
                    if (Patch.PARTICIPANT_ID.equals(patch.getParent())) {
                        if (StringUtils.isNotBlank(patch.getFieldId())) {
                            //abstraction related change
                            //multiple value
                            if (patch.getNameValues() != null && !patch.getNameValues().isEmpty()) {
                                String primaryKeyId = null;
                                for (NameValue nameValue : patch.getNameValues()) {
                                    DBElement dbElement = patchUtil.getColumnNameMap().get(nameValue.getName());
                                    if (dbElement != null) {
                                        if (primaryKeyId == null) {
                                            primaryKeyId = AbstractionWrapper.createNewAbstractionFieldValue(patch.getParentId(), patch.getFieldId(), patch.getUser(), nameValue, dbElement);
                                        }
                                        if (!Patch.patch(primaryKeyId, patch.getUser(), nameValue, dbElement)) {
                                            return new RuntimeException("An error occurred while attempting to patch ");
                                        }
                                    }
                                    else {
                                        throw new RuntimeException("DBElement not found in ColumnNameMap: " + nameValue.getName());
                                    }
                                }
                                Map<String, String> map = new HashMap<>();
                                map.put(PRIMARY_KEY_ID, primaryKeyId);
                                //return map with nulls
                                GoogleAnalyticsMetricsTracker.getInstance().sendAnalyticsMetrics(null, GoogleAnalyticsMetrics.EVENT_CATEGORY_PATCH_DATA,
                                        GoogleAnalyticsMetrics.EVENT_ACTION_PATCH_DATA, GoogleAnalyticsMetrics.EVENT_LABEL_PATCH_DATA, null, 1);
                                return new Result(200, gson.toJson(map));
                            }
                            else {
                                //single value
                                DBElement dbElement = patchUtil.getColumnNameMap().get(patch.getNameValue().getName());
                                if (dbElement != null) {
                                    String primaryKeyId = AbstractionWrapper.createNewAbstractionFieldValue(patch.getParentId(), patch.getFieldId(), patch.getUser(), patch.getNameValue(), dbElement);
                                    Map<String, String> map = new HashMap<>();
                                    map.put(PRIMARY_KEY_ID, primaryKeyId);
                                    //return map with nulls
                                    return new Result(200, gson.toJson(map));
                                }
                                else {
                                    throw new RuntimeException("DBElement not found in ColumnNameMap: " + patch.getNameValue().getName());
                                }
                            }
                        }
                        else {
                            //medical record tracking related change
                            Number mrID = MedicalRecordUtil.isInstitutionTypeInDB(patch.getParentId());
                            if (mrID == null) {
                                // mr of that type doesn't exist yet, so create an institution and mr
                                MedicalRecordUtil.writeInstitutionIntoDb(patch.getParentId(), MedicalRecordUtil.NOT_SPECIFIED);
                                mrID = MedicalRecordUtil.isInstitutionTypeInDB(patch.getParentId());
                            }
                            if (mrID != null) {
                                // a mr of that type already exits, add oncHistoryDetails to it
                                String oncHistoryDetailId = OncHistoryDetail.createNewOncHistoryDetail(mrID.toString(), patch.getUser());
                                List<NameValue> nameValues = null;
                                Map<String, String> map = new HashMap<>();
                                //facility was added
                                if (patch.getNameValues() != null && !patch.getNameValues().isEmpty()) {
                                    for (NameValue nameValue : patch.getNameValues()) {
                                        DBElement dbElement = patchUtil.getColumnNameMap().get(nameValue.getName());
                                        if (dbElement != null) {
                                            if (!Patch.patch(oncHistoryDetailId, patch.getUser(), nameValue, dbElement)) {
                                                return new RuntimeException("An error occurred while attempting to patch ");
                                            }
                                        }
                                        else {
                                            throw new RuntimeException("DBElement not found in ColumnNameMap: " + nameValue.getName());
                                        }
                                    }
                                }
                                else {
                                    DBElement dbElement = patchUtil.getColumnNameMap().get(patch.getNameValue().getName());
                                    if (dbElement != null) {
                                        if (Patch.patch(oncHistoryDetailId, patch.getUser(), patch.getNameValue(), dbElement)) {
                                            nameValues = setWorkflowRelatedFields(patch);
                                            //set oncHistoryDetails created if it is a oncHistoryDetails value without a ID, otherwise created should already be set
                                            if (dbElement.getTableName().equals(DBConstants.DDP_ONC_HISTORY_DETAIL)) {
                                                NameValue oncHistoryCreated = OncHistory.setOncHistoryCreated(patch.getParentId(), patch.getUser());
                                                if (oncHistoryCreated != null && oncHistoryCreated.getValue() != null) {
                                                    nameValues.add(oncHistoryCreated);
                                                }
                                            }
                                        }
                                    }
                                    else {
                                        throw new RuntimeException("DBElement not found in ColumnNameMap: " + patch.getNameValue().getName());
                                    }
                                }

                                // add oncHistoryId and NameValues of objects changed by workflow to json and sent it back to UI
                                map.put("oncHistoryDetailId", oncHistoryDetailId);
                                nameValues.add(new NameValue("request", OncHistoryDetail.STATUS_REVIEW));
                                map.put(NAME_VALUE, gson.toJson(nameValues));
                                GoogleAnalyticsMetricsTracker.getInstance().sendAnalyticsMetrics(null,GoogleAnalyticsMetrics.EVENT_CATEGORY_PATCH_DATA,
                                        GoogleAnalyticsMetrics.EVENT_ACTION_PATCH_DATA, GoogleAnalyticsMetrics.EVENT_LABEL_PATCH_DATA, null, 1);
                                //return map with nulls
                                return new Result(200, gson.toJson(map));
                            }
                            else {
                                logger.error("No medical record id for oncHistoryDetails ");
                            }
                        }
                    }
                    else if (Patch.ONC_HISTORY_ID.equals(patch.getParent())) {
                        String tissueId = Tissue.createNewTissue(patch.getParentId(), patch.getUser());
                        DBElement dbElement = patchUtil.getColumnNameMap().get(patch.getNameValue().getName());
                        if (dbElement != null) {
                            if (Patch.patch(tissueId, patch.getUser(), patch.getNameValue(), dbElement)) {
                                List<NameValue> nameValues = setWorkflowRelatedFields(patch);
                                Map<String, String> map = new HashMap<>();
                                map.put("tissueId", tissueId);
                                if (!nameValues.isEmpty()) {
                                    map.put(NAME_VALUE, gson.toJson(nameValues));
                                }
                                GoogleAnalyticsMetricsTracker.getInstance().sendAnalyticsMetrics(null, GoogleAnalyticsMetrics.EVENT_CATEGORY_PATCH_DATA,
                                        GoogleAnalyticsMetrics.EVENT_ACTION_PATCH_DATA, GoogleAnalyticsMetrics.EVENT_LABEL_PATCH_DATA, null, 1);
                                    return new Result(200, gson.toJson(map));
                            }
                        }
                        else {
                            throw new RuntimeException("DBElement not found in ColumnNameMap: " + patch.getNameValue().getName());
                        }
                    }
                    else if (Patch.PARTICIPANT_DATA_ID.equals(patch.getParent())) {
                        String participantDataId = null;
                        Map<String, String> map = new HashMap<>();
                        DDPInstance ddpInstance = DDPInstance.getDDPInstance(patch.getRealm());
                        for (NameValue nameValue : patch.getNameValues()) {
                            DBElement dbElement = patchUtil.getColumnNameMap().get(nameValue.getName());
                            if (dbElement != null) {
                                if (participantDataId == null) {
                                    participantDataId = ParticipantData.createNewParticipantData(patch.getParentId(), ddpInstance.getDdpInstanceId(), patch.getFieldId(), String.valueOf(nameValue.getValue()), patch.getUser());
                                    map.put(ESObjectConstants.PARTICIPANT_DATA_ID, participantDataId);
                                }
                                else if (participantDataId != null) {
                                    Patch.patch(participantDataId, patch.getUser(), nameValue, dbElement);
                                }
                                if (patch.getActions() != null) {
                                    ESProfile profile = ElasticSearchUtil.getParticipantProfileByGuidOrAltPid(ddpInstance.getParticipantIndexES(), patch.getParentId())
                                            .orElseThrow(() -> new RuntimeException("Unable to find ES profile for participant: " + patch.getParentId()));
                                    for (Value action : patch.getActions()) {
                                        if (ESObjectConstants.ELASTIC_EXPORT_WORKFLOWS.equals(action.getType())) {
                                            writeESWorkflow(patch, nameValue, action, ddpInstance, profile.getParticipantGuid());
                                        }
                                        else if (EventTypeDao.EVENT.equals(action.getType())) {
                                            triggerParticipantEvent(ddpInstance, patch, action);
                                        }
                                    }
                                }
                            }
                        }
                        GoogleAnalyticsMetricsTracker.getInstance().sendAnalyticsMetrics(ddpInstance.getName(), GoogleAnalyticsMetrics.EVENT_CATEGORY_PATCH_DATA,
                                GoogleAnalyticsMetrics.EVENT_ACTION_PATCH_DATA, GoogleAnalyticsMetrics.EVENT_LABEL_PATCH_DATA, null, 1);
                        return new Result(200, gson.toJson(map));
                    }
                    else if (Patch.DDP_PARTICIPANT_ID.equals(patch.getParent())) {
                        //new additional value for pt which is not in ddp_participant and ddp_participant_record table yet
                        DDPInstance ddpInstance = DDPInstance.getDDPInstance(patch.getRealm());
                        int participantId = insertDdpParticipant(patch, ddpInstance);
                        insertDdpParticipantRecord(participantId);
                        if (participantId > 0) {
                            DBElement dbElement = patchUtil.getColumnNameMap().get(patch.getNameValue().getName());
                            if (dbElement == null) {
                                throw new RuntimeException("DBElement not found in ColumnNameMap: " + patch.getNameValue().getName());
                            }
                            Patch.patch(String.valueOf(participantId), patch.getUser(), patch.getNameValue(), dbElement);
                            GoogleAnalyticsMetricsTracker.getInstance().sendAnalyticsMetrics(ddpInstance.getName(), GoogleAnalyticsMetrics.EVENT_CATEGORY_PATCH_DATA,
                                    GoogleAnalyticsMetrics.EVENT_ACTION_PATCH_DATA, GoogleAnalyticsMetrics.EVENT_LABEL_PATCH_DATA, null, 1);
                            return new Result(200, gson.toJson(Map.of(PARTICIPANT_ID, String.valueOf(participantId))));
                        }
                    }
                }
                GoogleAnalyticsMetricsTracker.getInstance().sendAnalyticsMetrics(null, GoogleAnalyticsMetrics.EVENT_CATEGORY_PATCH_DATA,
                        GoogleAnalyticsMetrics.EVENT_ACTION_PATCH_DATA, GoogleAnalyticsMetrics.EVENT_LABEL_PATCH_DATA, null, 1);
                throw new RuntimeException("Id and parentId was null");
            }
            catch (DuplicateException e) {
                GoogleAnalyticsMetricsTracker.getInstance().sendAnalyticsMetrics(null, GoogleAnalyticsMetrics.EVENT_CATEGORY_PATCH_DATA,
                        GoogleAnalyticsMetrics.EVENT_ACTION_PATCH_DATA, GoogleAnalyticsMetrics.EVENT_LABEL_PATCH_DATA, null, 1);
                return new Result(500, "Duplicate value");
            }
            catch (Exception e) {
                GoogleAnalyticsMetricsTracker.getInstance().sendAnalyticsMetrics(null, GoogleAnalyticsMetrics.EVENT_CATEGORY_PATCH_DATA,
                        GoogleAnalyticsMetrics.EVENT_ACTION_PATCH_DATA, GoogleAnalyticsMetrics.EVENT_LABEL_PATCH_DATA, null, 1);
                throw new RuntimeException("An error occurred while attempting to patch ", e);
            }
        }
        else {
            GoogleAnalyticsMetricsTracker.getInstance().sendAnalyticsMetrics(null, GoogleAnalyticsMetrics.EVENT_CATEGORY_PATCH_DATA,
                    GoogleAnalyticsMetrics.EVENT_ACTION_PATCH_DATA, GoogleAnalyticsMetrics.EVENT_LABEL_PATCH_DATA, null, 1);
            response.status(500);
            return new Result(500, UserErrorMessages.NO_RIGHTS);
        }
    }

    private void controlWorkflowByEmail(Patch patch, NameValue nameValue, DDPInstance ddpInstance, ESProfile profile) {
        if (profile == null || nameValue.getValue() == null) {
            return;
        }
        try {
            Map<String, String> pData = gson.fromJson(nameValue.getValue().toString(), Map.class);
            org.broadinstitute.dsm.model.participant.data.ParticipantData participantData =
                    new org.broadinstitute.dsm.model.participant.data.ParticipantData(Integer.parseInt(patch.getId()),
                            patch.getParentId(), Integer.parseInt(ddpInstance.getDdpInstanceId()), patch.getFieldId(),
                            pData);

            if (participantData.hasFamilyMemberApplicantEmail(profile)) {
                logger.info("Email in patch data matches participant profile email, will update workflows");
                int ddpInstanceIdByGuid = Integer.parseInt(ddpInstance.getDdpInstanceId());
                FieldSettings fieldSettings = new FieldSettings();
                pData.forEach((columnName,columnValue) -> {
                    if (!fieldSettings.isColumnExportable(ddpInstanceIdByGuid, columnName)) return;
                    if (!patch.getFieldId().contains(org.broadinstitute.dsm.model.participant.data.ParticipantData.FIELD_TYPE)) return;
                    // Use participant guid here to avoid multiple ES lookups.
                    ElasticSearchUtil.writeWorkflow(WorkflowForES.createInstanceWithStudySpecificData(ddpInstance,
                            profile.getParticipantGuid(), columnName, columnValue, new WorkflowForES.StudySpecificData(
                                    pData.get(FamilyMemberConstants.COLLABORATOR_PARTICIPANT_ID),
                                    pData.get(FamilyMemberConstants.FIRSTNAME),
                                    pData.get(FamilyMemberConstants.LASTNAME))), false);
                });
            } else {
                logger.info("Email in patch data does not match participant profile email, will remove workflows");
                Map<String, Object> esMap = ElasticSearchUtil
                        .getObjectsMap(ddpInstance.getParticipantIndexES(), profile.getParticipantGuid(),
                                ESObjectConstants.WORKFLOWS);
                if (Objects.isNull(esMap) || esMap.isEmpty()) return;
                CopyOnWriteArrayList<Map<String, Object>> workflowsList = new CopyOnWriteArrayList<>((List<Map<String, Object>>)esMap.get(ESObjectConstants.WORKFLOWS));
                int startingSize = workflowsList.size();
                workflowsList.forEach(workflow -> {
                    Map<String, String> workflowDataMap = (Map<String, String>) workflow.get(ESObjectConstants.DATA);
                    String collaboratorParticipantId = workflowDataMap.get(ESObjectConstants.SUBJECT_ID);
                    if (Objects.isNull(collaboratorParticipantId)) return;
                    if (collaboratorParticipantId.equals(pData.get(FamilyMemberConstants.COLLABORATOR_PARTICIPANT_ID))) {
                        workflowsList.remove(workflow);
                    }
                });
                if (startingSize != workflowsList.size()) {
                    esMap.put(ESObjectConstants.WORKFLOWS, workflowsList);
                    // Use participant guid here to avoid another ES lookup.
                    ElasticSearchUtil.updateRequest(profile.getParticipantGuid(), ddpInstance.getParticipantIndexES(), esMap);
                }
            }
        } catch (JsonSyntaxException ignored) {
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void insertDdpParticipantRecord(int participantId) {
        ParticipantRecordDto participantRecordDto =
                new ParticipantRecordDto.Builder(participantId, System.currentTimeMillis())
                        .withChangedBy("SYSTEM")
                        .builder();
        new ParticipantRecordDao().create(participantRecordDto);
    }

    private int insertDdpParticipant(Patch patch, DDPInstance ddpInstance) {
        ParticipantDto participantDto =
                new ParticipantDto.Builder(Integer.parseInt(ddpInstance.getDdpInstanceId()), System.currentTimeMillis())
                        .withDdpParticipantId(patch.getParentId())
                        .withLastVersion(0)
                        .withLastVersionDate("")
                        .withChangedBy(patch.getUser())
                        .build();
        return new ParticipantDao().create(participantDto);
    }

    private void writeDSMRecordsToES(@NonNull Patch patch, DDPInstance ddpInstance) {
        NameValue nameValue = patch.getNameValue();
        String name = nameValue.getName().substring(nameValue.getName().lastIndexOf('.') + 1);
        String type = null;
        if (nameValue.getName().indexOf('.') != -1) {
            type = nameValue.getName().substring(0, nameValue.getName().indexOf('.'));
        }
        else {
            return;
        }
        String value = nameValue.getValue().toString();
        Map<String, Object> nameValueMap = new HashMap<>();
        nameValueMap.put(name, value);
        if (DBConstants.DDP_MEDICAL_RECORD_ALIAS.equals(type)) {
            if (ESObjectConstants.MEDICAL_RECORDS_FIELD_NAMES.contains(name)) {
                ElasticSearchUtil.writeDsmRecord(ddpInstance, Integer.parseInt(patch.getId()), patch.getDdpParticipantId(),
                        ESObjectConstants.MEDICAL_RECORDS, ESObjectConstants.MEDICAL_RECORDS_ID, nameValueMap);
            }
        }
        else if (DBConstants.DDP_ONC_HISTORY_DETAIL_ALIAS.equals(type)) {
            if (ESObjectConstants.TISSUE_RECORDS_FIELD_NAMES.contains(name)) {
                if (PARTICIPANT_ID.equals(patch.getParent())) {
                    ElasticSearchUtil.writeDsmRecord(ddpInstance, Integer.parseInt(patch.getId()), patch.getDdpParticipantId(),
                            ESObjectConstants.TISSUE_RECORDS, ESObjectConstants.TISSUE_RECORDS_ID, nameValueMap);
                }
            }
        }
    }

    private void writeESWorkflow(@NonNull Patch patch, @NonNull NameValue nameValue, @NonNull Value action, DDPInstance ddpInstance, String esParticipantId) {
        String status = nameValue.getValue() != null ? String.valueOf(nameValue.getValue()) : null;
        if (StringUtils.isBlank(status)) {
            return;
        }
        Map<String, String> data = gson.fromJson(status, new TypeToken<Map<String, String>>() {
        }.getType());
        if (StringUtils.isNotBlank(action.getValue())) {
            if (!patch.getFieldId().contains(FamilyMemberConstants.PARTICIPANTS)) {
                ElasticSearchUtil.writeWorkflow(WorkflowForES.createInstance(ddpInstance, esParticipantId, action.getName(), action.getValue()), false);
            }
            else if (ParticipantUtil.matchesApplicantEmail(data.get(FamilyMemberConstants.COLLABORATOR_PARTICIPANT_ID),
                    participantDataDao.getParticipantDataByParticipantId(patch.getParentId()))) {
                ElasticSearchUtil.writeWorkflow(WorkflowForES.createInstanceWithStudySpecificData(ddpInstance,
                        esParticipantId, action.getName(), data.get(action.getName()), new WorkflowForES.StudySpecificData(
                                data.get(FamilyMemberConstants.COLLABORATOR_PARTICIPANT_ID),
                                data.get(FamilyMemberConstants.FIRSTNAME),
                                data.get(FamilyMemberConstants.LASTNAME))), false);
            }
        }
        else if (StringUtils.isNotBlank(action.getName()) && data.containsKey(action.getName())) {
            if (!patch.getFieldId().contains(FamilyMemberConstants.PARTICIPANTS)) {
                ElasticSearchUtil.writeWorkflow(WorkflowForES.createInstance(ddpInstance, esParticipantId, action.getName(), data.get(action.getName())), false);
            }
            else if (ParticipantUtil.matchesApplicantEmail(data.get(FamilyMemberConstants.COLLABORATOR_PARTICIPANT_ID),
                    participantDataDao.getParticipantDataByParticipantId(patch.getParentId()))) {
                ElasticSearchUtil.writeWorkflow(WorkflowForES.createInstanceWithStudySpecificData(ddpInstance,
                        esParticipantId, action.getName(), data.get(action.getName()), new WorkflowForES.StudySpecificData(
                                data.get(FamilyMemberConstants.COLLABORATOR_PARTICIPANT_ID),
                                data.get(FamilyMemberConstants.FIRSTNAME),
                                data.get(FamilyMemberConstants.LASTNAME))), false);
            }
        }
    }

    private List<NameValue> setWorkflowRelatedFields(@NonNull Patch patch) {
        List<NameValue> nameValues = new ArrayList<>();
        //mr request workflow
        if (patch.getNameValue().getName().equals("m.faxSent")) {
            nameValues.add(setAdditionalValue("m.faxSentBy", patch, patch.getUser()));
            nameValues.add(setAdditionalValue("m.faxConfirmed", patch, patch.getNameValue().getValue()));
        }
        else if (patch.getNameValue().getName().equals("m.faxSent2")) {
            nameValues.add(setAdditionalValue("m.faxSent2By", patch, patch.getUser()));
            nameValues.add(setAdditionalValue("m.faxConfirmed2", patch, patch.getNameValue().getValue()));
        }
        else if (patch.getNameValue().getName().equals("m.faxSent3")) {
            nameValues.add(setAdditionalValue("m.faxSent3By", patch, patch.getUser()));
            nameValues.add(setAdditionalValue("m.faxConfirmed3", patch, patch.getNameValue().getValue()));
        }
        //tissue request workflow
        else if (patch.getNameValue().getName().equals("oD.tFaxSent")) {
            nameValues.add(setAdditionalValue("oD.tFaxSentBy", patch, patch.getUser()));
            nameValues.add(setAdditionalValue("oD.tFaxConfirmed", patch, patch.getNameValue().getValue()));
            nameValues.add(setAdditionalValue("oD.request", patch, "sent"));
        }
        else if (patch.getNameValue().getName().equals("oD.tFaxSent2")) {
            nameValues.add(setAdditionalValue("oD.tFaxSent2By", patch, patch.getUser()));
            nameValues.add(setAdditionalValue("oD.tFaxConfirmed2", patch, patch.getNameValue().getValue()));
            nameValues.add(setAdditionalValue("oD.request", patch, "sent"));
        }
        else if (patch.getNameValue().getName().equals("oD.tFaxSent3")) {
            nameValues.add(setAdditionalValue("oD.tFaxSent3By", patch, patch.getUser()));
            nameValues.add(setAdditionalValue("oD.tFaxConfirmed3", patch, patch.getNameValue().getValue()));
            nameValues.add(setAdditionalValue("oD.request", patch, "sent"));
        }
        else if (patch.getNameValue().getName().equals("oD.tissueReceived")) {
            nameValues.add(setAdditionalValue("oD.request", patch, "received"));
        }
        else if (patch.getNameValue().getName().equals("t.tissueReturnDate")) {
            if (StringUtils.isNotBlank(patch.getNameValue().getValue().toString())) {
                nameValues.add(setAdditionalValue("oD.request", new Patch(patch.getParentId(), PARTICIPANT_ID,
                        null, patch.getUser(), patch.getNameValue(), patch.getNameValues(), patch.getDdpParticipantId()), "returned"));
            }
            else {
                Boolean hasReceivedDate = OncHistoryDetail.hasReceivedDate(patch);

                if (hasReceivedDate) {
                    nameValues.add(setAdditionalValue("oD.request", new Patch(patch.getParentId(), PARTICIPANT_ID,
                            null, patch.getUser(), patch.getNameValue(), patch.getNameValues(), patch.getDdpParticipantId()), "received"));
                }
                else {
                    nameValues.add(setAdditionalValue("oD.request", new Patch(patch.getParentId(), PARTICIPANT_ID,
                            null, patch.getUser(), patch.getNameValue(), patch.getNameValues(), patch.getDdpParticipantId()), "sent"));
                }
            }
        }
        else if (patch.getNameValue().getName().equals("oD.unableToObtain") && (boolean) patch.getNameValue().getValue()) {
        }
        else if (patch.getNameValue().getName().equals("oD.unableToObtain") && !(boolean) patch.getNameValue().getValue()) {
            Boolean hasReceivedDate = OncHistoryDetail.hasReceivedDate(patch);

            if (hasReceivedDate) {
                nameValues.add(setAdditionalValue("oD.request", new Patch(patch.getId(), PARTICIPANT_ID,
                        patch.getParentId(), patch.getUser(), patch.getNameValue(), patch.getNameValues(), patch.getDdpParticipantId()), "received"));
            }
            else {
                nameValues.add(setAdditionalValue("oD.request", new Patch(patch.getId(), PARTICIPANT_ID,
                        patch.getParentId(), patch.getUser(), patch.getNameValue(), patch.getNameValues(), patch.getDdpParticipantId()), "sent"));
            }
        }
        return nameValues;
    }

    private NameValue setAdditionalValue(String additionalValue, @NonNull Patch patch, @NonNull Object value) {
        DBElement dbElement = patchUtil.getColumnNameMap().get(additionalValue);
        if (dbElement != null) {
            NameValue nameValue = new NameValue(additionalValue, value);
            Patch.patch(patch.getId(), patch.getUser(), nameValue, dbElement);
            return nameValue;
        }
        else {
            throw new RuntimeException("DBElement not found in ColumnNameMap: " + additionalValue);
        }
    }

    private void triggerParticipantEvent(DDPInstance ddpInstance, Patch patch, Value action){
        Optional<EventTypeDto> eventType = eventTypeDao.getEventTypeByEventTypeAndInstanceId(action.getName(), ddpInstance.getDdpInstanceId());
        eventType.ifPresent(eventTypeDto -> {
            boolean participantHasTriggeredEventByEventType = eventDao.hasTriggeredEventByEventTypeAndDdpParticipantId(action.getName(), patch.getParentId()).orElse(false);
            if (!participantHasTriggeredEventByEventType) {
                inTransaction((conn) -> {
                    EventUtil.triggerDDP(conn, eventType, patch.getParentId());
                    return null;
                });
            }
            else {
                logger.info("Participant " + patch.getParentId() + " was already triggered for event type " + action.getName());
            }
        });
    }
}
