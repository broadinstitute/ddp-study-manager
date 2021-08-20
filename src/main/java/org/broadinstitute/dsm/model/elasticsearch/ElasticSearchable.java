package org.broadinstitute.dsm.model.elasticsearch;

import java.util.List;

public interface ElasticSearchable {

    ElasticSearch getParticipantsWithinRange(String esParticipantsIndex, int from, int to, String sortField, String sortDir);

    ElasticSearch getParticipantsByIds(String esParticipantsIndex, List<String> participantIds, String sortField, String sortDir);

    long getParticipantsSize(String esParticipantsIndex);

    ElasticSearch getParticipantsByRangeAndFilter(String esParticipantsIndex, int from, int to, String sortField, String sortDir, String filter);

    ElasticSearch getParticipantsByRangeAndIds(String participantIndexES, int from, int to, String sortField, String sortDir, List<String> participantIds);

    ElasticSearchParticipantDto getParticipantByShortId(String esParticipantsIndex, String shortId);

    ElasticSearch getAllParticipantsDataByInstanceIndex(String esParticipantsIndex, String sortField, String sortDir);

}
