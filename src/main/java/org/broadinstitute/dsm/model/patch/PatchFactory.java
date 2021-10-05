package org.broadinstitute.dsm.model.patch;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.dsm.model.Patch;
import org.broadinstitute.dsm.util.NotificationUtil;

public class PatchFactory {

    public static BasePatch makePatch(Patch patch, NotificationUtil notificationUtil) {
        BasePatch patcher = new NullPatch();
        if (hasPrimaryKey(patch)) {
            patcher = new PrimaryKeyPatch(patch, notificationUtil);
        }
        // switch cases here
        return patcher;
    }

    private static boolean hasPrimaryKey(Patch patch) {
        return StringUtils.isNotBlank(patch.getId());
    }





}
