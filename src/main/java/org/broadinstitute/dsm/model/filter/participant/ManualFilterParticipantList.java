package org.broadinstitute.dsm.model.filter.participant;

import java.util.List;
import java.util.Objects;

import org.broadinstitute.dsm.model.ParticipantWrapper;
import org.broadinstitute.dsm.util.PatchUtil;
import spark.QueryParamsMap;

public class ManualFilterParticipantList extends BaseFilterParticipantList{


    public ManualFilterParticipantList(String json) {
        super(json);
    }

    @Override
    public List<ParticipantWrapper> filter(QueryParamsMap queryParamsMap) {
        prepareNeccesaryData(queryParamsMap);
        return filterParticipantList(filters, PatchUtil.getColumnNameMap(), ddpInstance);
    }

}
