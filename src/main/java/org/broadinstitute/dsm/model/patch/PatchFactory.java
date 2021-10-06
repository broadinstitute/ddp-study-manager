package org.broadinstitute.dsm.model.patch;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.dsm.model.Patch;
import org.broadinstitute.dsm.util.NotificationUtil;

public class PatchFactory {

    public static BasePatch makePatch(Patch patch, NotificationUtil notificationUtil) {
        BasePatch patcher = new NullPatch();
        if (hasPrimaryKey(patch)) {
            patcher = new PrimaryKeyPatch(patch, notificationUtil);
        } else if (isParentWithPrimaryKey(patch)) {
            if (isParentParticipantId(patch)) {
                if (isMedicalRecordAbstractionFieldId(patch)) {
                    patcher = new AbstractionPatch(patch);
                } else {
                    patcher = new MedicalRecordPatch(patch);
                }
            }
        }
        // switch cases here
        return patcher;
    }

    private static boolean hasPrimaryKey(Patch patch) {
        return StringUtils.isNotBlank(patch.getId());
    }

    private static boolean isParentWithPrimaryKey(Patch patch) {
        return StringUtils.isNotBlank(patch.getParent()) && StringUtils.isNotBlank(patch.getParentId());
    }

    private static boolean isParentParticipantId(Patch patch) {
        return Patch.PARTICIPANT_ID.equals(patch.getParent());
    }

    private static boolean isMedicalRecordAbstractionFieldId(Patch patch) {
        return StringUtils.isNotBlank(patch.getFieldId());
    }
}
