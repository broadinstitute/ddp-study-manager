package org.broadinstitute.dsm.model.patch;

import org.broadinstitute.dsm.db.structure.DBElement;
import org.broadinstitute.dsm.model.NameValue;
import org.broadinstitute.dsm.model.Patch;

import java.util.Optional;

public class PrimaryKeyPatch extends BasePatch {

    public PrimaryKeyPatch(Patch patch) {
        super(patch);
    }

    @Override
    Optional<Object> processSingleNameValue(NameValue nameValue, DBElement dbElement) {

        if (!Patch.patch(patch.getId(), patch.getUser(), nameValue, dbElement)) {
            throw new RuntimeException("An error occurred while attempting to patch ");
        }
        if (hasQuestion(nameValue)) {
            sendNotificationEmailAndUpdateStatus(patch, nameValues, nameValue, dbElement);
        }
        controlWorkflowByEmail(patch, nameValue, ddpInstance, profile);
        if (patch.getActions() != null) {
            writeESWorkflowElseTriggerParticipantEvent(patch, ddpInstance, profile, nameValue);
        }

        return Optional.empty();
    }
}
