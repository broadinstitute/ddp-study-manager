package org.broadinstitute.dsm.model.elastic;

import org.broadinstitute.dsm.util.ParticipantUtil;

public class Util {

    public static String getQueryTypeFromId(String id) {
        String type;
        if (ParticipantUtil.isHruid(id)) {
            type = "profile.hruid";
        } else if (ParticipantUtil.isGuid(id)){
            type = "profile.guid";
        } else if (ParticipantUtil.isLegacyAltPid(id)) {
            type = "profile.legacyAltPid";
        } else {
            type = "profile.legacyShortId";
        }
        return type;
    }
}
