package org.broadinstitute.dsm.model.elastic.search;

import org.elasticsearch.index.query.AbstractQueryBuilder;

import java.util.List;
import java.util.logging.Filter;

public interface ElasticSearchable {


    ElasticSearch getParticipantsWithinRange(String esParticipantsIndex, int from, int to);

    ElasticSearch getParticipantsByIds(String esParticipantsIndex, List<String> participantIds);

    long getParticipantsSize(String esParticipantsIndex);

    ElasticSearch getParticipantsByRangeAndFilter(String esParticipantsIndex, int from, int to, AbstractQueryBuilder queryBuilder);

    ElasticSearch getParticipantsByRangeAndIds(String participantIndexES, int from, int to, List<String> participantIds);

    ElasticSearchParticipantDto getParticipantById(String esParticipantsIndex, String id);

    ElasticSearch getAllParticipantsDataByInstanceIndex(String esParticipantsIndex);

    default void setDeserializer(Deserializer deserializer) {

    }

    default void setSortBy(Filter sortBy) {

    }

}
