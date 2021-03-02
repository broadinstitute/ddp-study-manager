package org.broadinstitute.dsm.util;

import lombok.NonNull;

public class ParticipantUtil {


    public static boolean isHruid(@NonNull String participantId) {
        final String hruidCheck = "^P\\w{5}$";
        return participantId.matches(hruidCheck);
    }
}
