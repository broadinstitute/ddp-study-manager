package org.broadinstitute.dsm.model.filter.participant;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.dsm.model.Filter;
import org.broadinstitute.dsm.model.ParticipantWrapper;
import org.broadinstitute.dsm.statics.RequestParameter;
import org.broadinstitute.dsm.util.PatchUtil;
import spark.QueryParamsMap;

public class SavedFilterParticipantList extends BaseFilterParticipantList{
    @Override
    public List<ParticipantWrapper> filter(QueryParamsMap queryParamsMap) {
        prepareNeccesaryData(queryParamsMap);
        List<ParticipantWrapper> participantWrapperList = Collections.emptyList();
        if (StringUtils.isBlank(queryParamsMap.get(RequestParameter.FILTERS).value())) return participantWrapperList;
        Filter[] filters = GSON.fromJson(queryParamsMap.get(RequestParameter.FILTERS).value(), Filter[].class);
        if (filters != null) {
            participantWrapperList = filterParticipantList(filters, PatchUtil.getColumnNameMap(), ddpInstance);
        }
        return participantWrapperList;
    }
}
