package org.broadinstitute.dsm.model.patch;

import static org.broadinstitute.ddp.db.TransactionWrapper.inTransaction;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.dsm.db.DDPInstance;
import org.broadinstitute.dsm.db.dao.ddp.participant.ParticipantDataDao;
import org.broadinstitute.dsm.db.dao.queue.EventDao;
import org.broadinstitute.dsm.db.dao.settings.EventTypeDao;
import org.broadinstitute.dsm.db.dto.settings.EventTypeDto;
import org.broadinstitute.dsm.db.structure.DBElement;
import org.broadinstitute.dsm.export.WorkflowForES;
import org.broadinstitute.dsm.model.NameValue;
import org.broadinstitute.dsm.model.Patch;
import org.broadinstitute.dsm.model.Value;
import org.broadinstitute.dsm.model.elasticsearch.ESProfile;
import org.broadinstitute.dsm.model.participant.data.FamilyMemberConstants;
import org.broadinstitute.dsm.statics.ESObjectConstants;
import org.broadinstitute.dsm.util.ElasticSearchUtil;
import org.broadinstitute.dsm.util.EventUtil;
import org.broadinstitute.dsm.util.NotificationUtil;
import org.broadinstitute.dsm.util.ParticipantUtil;
import org.broadinstitute.dsm.util.PatchUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public abstract class BasePatch implements Patchable {

    static final Logger logger = LoggerFactory.getLogger(BasePatch.class);

    protected static final String PARTICIPANT_ID = "participantId";
    protected static final String PRIMARY_KEY_ID = "primaryKeyId";
    protected static final String NAME_VALUE = "NameValue";
    protected static final String STATUS = "status";
    protected static final Gson GSON = new GsonBuilder().serializeNulls().create();


    protected Patch patch;
    protected ESProfile profile;
    protected DDPInstance ddpInstance;

    public BasePatch() {
    }

    public BasePatch(Patch patch) {
        this.patch = patch;
        prepareNecessaryData();
    }

    private void prepareNecessaryData() {
        ddpInstance = DDPInstance.getDDPInstance(patch.getRealm());
        profile = ElasticSearchUtil.getParticipantProfileByGuidOrAltPid(ddpInstance.getParticipantIndexES(), patch.getDdpParticipantId())
                .orElse(new ESProfile());
    }

    abstract Optional<NameValue> processSingleNameValue(NameValue nameValue, DBElement dbElement);

    List<NameValue> processMultipleNameValues(List<NameValue> nameValues) {
        List<NameValue> updatedNameValues = new ArrayList<>();
        for (NameValue nameValue : patch.getNameValues()) {
            DBElement dbElement = PatchUtil.getColumnNameMap().get(nameValue.getName());
            if (dbElement != null) {
                processSingleNameValue(nameValue, dbElement).ifPresent(updatedNameValues::add);
            }
            else {
                throw new RuntimeException("DBElement not found in ColumnNameMap: " + nameValue.getName());
            }
        }
        return updatedNameValues;
    }

    protected boolean hasQuestion(NameValue nameValue) {
        return nameValue.getName().contains("question");
    }


    protected boolean hasProfileAndESWorkflowType(ESProfile profile, Value action) {
        return ESObjectConstants.ELASTIC_EXPORT_WORKFLOWS.equals(action.getType()) && profile != null;
    }

    protected void writeESWorkflow(@NonNull Patch patch, @NonNull NameValue nameValue, @NonNull Value action, DDPInstance ddpInstance,
                           String esParticipantId) {
        String status = nameValue.getValue() != null ? String.valueOf(nameValue.getValue()) : null;
        if (StringUtils.isBlank(status)) {
            return;
        }
        Map<String, String> data = GSON.fromJson(status, new TypeToken<Map<String, String>>() {
        }.getType());
        final ParticipantDataDao participantDataDao = new ParticipantDataDao();
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

    protected void triggerParticipantEvent(DDPInstance ddpInstance, Patch patch, Value action){
        final EventDao eventDao = new EventDao();
        final EventTypeDao eventTypeDao = new EventTypeDao();
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
