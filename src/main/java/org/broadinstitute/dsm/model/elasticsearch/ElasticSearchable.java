package org.broadinstitute.dsm.model.elasticsearch;

import java.util.List;

public interface ElasticSearchable {


    List<ElasticSearch> getParticipantsWithinRange(String esParticipantsIndex, int from, int to);

    List<ElasticSearch> getParticipantsByIds(String esParticipantsIndex, List<String> participantIds);

    long getParticipantsSize(String esParticipantsIndex);

}
