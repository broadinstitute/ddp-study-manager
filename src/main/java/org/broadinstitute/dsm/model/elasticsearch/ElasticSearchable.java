package org.broadinstitute.dsm.model.elasticsearch;

import java.util.List;

public interface ElasticSearchable {

    ElasticSearch getParticipantsWithinRange(String esParticipantsIndex, int from, int to);

    ElasticSearch getParticipantsByIds(String esParticipantsIndex, List<String> participantIds);

    long getParticipantsSize(String esParticipantsIndex);

    ElasticSearch getParticipantsByRangeAndFilter(String esParticipantsIndex, int from, int to, String filter);

    ElasticSearch getParticipantsByRangeAndIds(String participantIndexES, int from, int to, List<String> participantIds);

    ElasticSearchParticipantDto getParticipantByShortId(String esParticipantsIndex, String shortId);

    ElasticSearch getAllParticipantsDataByInstanceIndex(String esParticipantsIndex);

    ElasticSearch getProxiesByFilter(String esParticipantsIndex, String filter);

}
