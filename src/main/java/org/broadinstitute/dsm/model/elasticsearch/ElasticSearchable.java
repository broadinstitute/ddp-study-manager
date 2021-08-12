package org.broadinstitute.dsm.model.elasticsearch;

import java.util.List;
import java.util.Optional;

public interface ElasticSearchable {


    ElasticSearch getParticipantsWithinRange(String esParticipantsIndex, int from, int to);

    ElasticSearch getParticipantsByIds(String esParticipantsIndex, List<String> participantIds);

    long getParticipantsSize(String esParticipantsIndex);

    ElasticSearch getParticipantsByRangeAndFilter(String esParticipantsIndex, int from, int to, String filter);

    ElasticSearch getParticipantsByRangeAndIds(String participantIndexES, int from, int to, List<String> participantIds);

}
