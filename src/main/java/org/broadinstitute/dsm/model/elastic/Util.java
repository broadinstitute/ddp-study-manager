package org.broadinstitute.dsm.model.elastic;

import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.dsm.db.structure.DBElement;
import org.broadinstitute.dsm.model.elastic.export.BaseGenerator;
import org.broadinstitute.dsm.statics.ESObjectConstants;
import org.broadinstitute.dsm.util.ElasticSearchUtil;
import org.broadinstitute.dsm.util.ParticipantUtil;
import org.broadinstitute.dsm.util.PatchUtil;

public class Util {

    public static final Map<String, BaseGenerator.PropertyInfo> TABLE_ALIAS_MAPPINGS = Map.of(
            "m", new BaseGenerator.PropertyInfo(ESObjectConstants.MEDICAL_RECORDS, true),
            "t", new BaseGenerator.PropertyInfo(ESObjectConstants.TISSUE_RECORDS, true),
            "oD", new BaseGenerator.PropertyInfo(ESObjectConstants.ONC_HISTORY_DETAIL_RECORDS, true),
            "d", new BaseGenerator.PropertyInfo(ESObjectConstants.PARTICIPANT_DATA, true),
            "r", new BaseGenerator.PropertyInfo(ESObjectConstants.PARTICIPANT_RECORD, false),
            "p", new BaseGenerator.PropertyInfo(ESObjectConstants.PARTICIPANT, false),
            "o", new BaseGenerator.PropertyInfo(ESObjectConstants.ONC_HISTORY, false)
            );

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

    public static DBElement getDBElement(String fieldName) {
        return PatchUtil.getColumnNameMap().get(Objects.requireNonNull(fieldName));
    }
}
