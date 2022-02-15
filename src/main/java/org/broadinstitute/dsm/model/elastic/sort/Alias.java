package org.broadinstitute.dsm.model.elastic.sort;

import lombok.Getter;
import org.broadinstitute.dsm.statics.ESObjectConstants;
import org.broadinstitute.dsm.util.ElasticSearchUtil;

@Getter
public enum Alias {

    K(ESObjectConstants.KIT_REQUEST_SHIPPING,true),
    M(ESObjectConstants.MEDICAL_RECORD,true),
    OD(ESObjectConstants.ONC_HISTORY_DETAIL,true),
    P(ESObjectConstants.PARTICIPANT,false),
    O(ESObjectConstants.ONC_HISTORY,false),
    D(ESObjectConstants.PARTICIPANT_DATA,true),
    T(ESObjectConstants.TISSUE,true),
    R(ESObjectConstants.PARTICIPANT,false),
    STATUS(ESObjectConstants.STATUS,false),
    PROFILE(ElasticSearchUtil.PROFILE,false),
    ADDRESS(ElasticSearchUtil.ADDRESS,false),
    INVITATIONS(ElasticSearchUtil.INVITATIONS, true),
    PROXIES(ElasticSearchUtil.PROXIES,true),
    ACTIVITIES(ElasticSearchUtil.ACTIVITIES, true);

    Alias(String value, boolean isCollection) {
        this.value = value;
        this.isCollection = isCollection;
    }

    private boolean isCollection;
    private String value;

    public static Alias of(String name) {
        Alias alias;
        try {
            alias = valueOf(name.toUpperCase());
        } catch (IllegalArgumentException iae) {
            alias = ACTIVITIES;
        }
        return alias;
    }

}
