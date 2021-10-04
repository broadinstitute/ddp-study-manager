package org.broadinstitute.dsm.model.patch;

import org.broadinstitute.dsm.db.dao.user.UserDao;
import org.broadinstitute.dsm.db.dto.user.UserDto;
import org.broadinstitute.dsm.db.structure.DBElement;
import org.broadinstitute.dsm.model.NameValue;
import org.broadinstitute.dsm.model.Patch;
import org.broadinstitute.dsm.util.NotificationUtil;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class PrimaryKeyPatch extends BasePatch {

    private NotificationUtil notificationUtil;

    public PrimaryKeyPatch(Patch patch, NotificationUtil notificationUtil) {
        super(patch);
        this.notificationUtil = notificationUtil;
    }

    @Override
    Optional<NameValue> processSingleNameValue(NameValue nameValue, DBElement dbElement) {
        Optional<NameValue> maybeUpdatedNameValue = Optional.empty();
        if (!Patch.patch(patch.getId(), patch.getUser(), nameValue, dbElement)) {
            throw new RuntimeException("An error occurred while attempting to patch ");
        }
        if (hasQuestion(nameValue)) {
            maybeUpdatedNameValue = sendNotificationEmailAndUpdateStatus(patch, nameValue, dbElement);
        }
        controlWorkflowByEmail(patch, nameValue, ddpInstance, profile);
        if (patch.getActions() != null) {
            writeESWorkflowElseTriggerParticipantEvent(patch, ddpInstance, profile, nameValue);
        }

        return maybeUpdatedNameValue;
    }

    private Optional<NameValue> sendNotificationEmailAndUpdateStatus(Patch patch, NameValue nameValue, DBElement dbElement) {
        Optional<NameValue> maybeUpdatedNameValue = Optional.empty();
        UserDto userDto = new UserDao().getUserByEmail(patch.getUser()).orElseThrow();
        JSONObject jsonObject = new JSONObject(nameValue.getValue().toString());
        JSONArray questionArray = new JSONArray(jsonObject.get("questions").toString());
        boolean writeBack = false;
        for (int i = 0; i < questionArray.length(); i++) {
            JSONObject question = questionArray.getJSONObject(i);
            if (isSent(question)) {
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
                throw new RuntimeException("An error occurred while attempting to patch ");
            }
            maybeUpdatedNameValue = Optional.of(nameValue);
        }
        return maybeUpdatedNameValue;
    }

    private boolean isSent(JSONObject question) {
        return question.optString(STATUS) != null && question.optString(STATUS).equals("sent");
    }

}
