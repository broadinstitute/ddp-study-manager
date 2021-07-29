package org.broadinstitute.dsm.model.filter.participant;

import java.util.List;

import org.broadinstitute.dsm.model.ParticipantWrapper;
import org.broadinstitute.dsm.model.filter.Filterable;
import spark.QueryParamsMap;

public class FilterParticipantList implements Filterable<ParticipantWrapper> {


    @Override
    public List<ParticipantWrapper> filter(QueryParamsMap queryParamsMap) {
        return null;
    }
}
