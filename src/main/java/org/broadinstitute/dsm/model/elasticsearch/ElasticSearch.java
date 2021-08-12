package org.broadinstitute.dsm.model.elasticsearch;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.dsm.util.ElasticSearchUtil;
import org.broadinstitute.dsm.util.ParticipantUtil;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.core.CountRequest;
import org.elasticsearch.client.core.CountResponse;
import org.elasticsearch.index.query.AbstractQueryBuilder;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Setter
public class ElasticSearch implements ElasticSearchable {

    private static final Logger logger = LoggerFactory.getLogger(ElasticSearch.class);
    private static final Gson GSON = new Gson();

    List<ElasticSearchParticipantDto> esParticipants;
    long totalCount;

    public List<ElasticSearchParticipantDto> getEsParticipants() {
        if (Objects.isNull(esParticipants)) {
            esParticipants = Collections.emptyList();
        }
        return esParticipants;
    }

    public long getTotalCount() {
        return totalCount;
    }

    public static Optional<ElasticSearchParticipantDto> parseSourceMap(Map<String, Object> sourceMap) {
        if (sourceMap == null) return Optional.of(new ElasticSearchParticipantDto.Builder().build());
        ElasticSearchParticipantDto.Builder builder = new ElasticSearchParticipantDto.Builder();
        for (Map.Entry<String, Object> entry : sourceMap.entrySet()) {
            switch (entry.getKey()) {
                case "address":
                    builder.withAddress(GSON.fromJson(GSON.toJson(entry.getValue()), ESAddress.class));
                    break;
                case "medicalProviders":
                    builder.withMedicalProviders(GSON.fromJson(GSON.toJson(entry.getValue()), new TypeToken<List<Object>>() {}.getType()));
                    break;
                case "invitations":
                    builder.withInvitations(GSON.fromJson(GSON.toJson(entry.getValue()), new TypeToken<List<Object>>() {}.getType()));
                    break;
                case "activities":
                    builder.withActivities(GSON.fromJson(GSON.toJson(entry.getValue()), new TypeToken<List<ESActivities>>() {}.getType()));
                    break;
                case "statusTimeStamp":
                    builder.withStatusTimeStamp(GSON.fromJson(GSON.toJson(entry.getValue()), Long.class));
                    break;
                case "profile":
                    builder.withProfile(GSON.fromJson(GSON.toJson(entry.getValue()), ESProfile.class));
                    break;
                case "files":
                    builder.withFiles(GSON.fromJson(GSON.toJson(entry.getValue()), new TypeToken<List<Object>>() {}.getType()));
                    break;
                case "proxies":
                    builder.withProxies(GSON.fromJson(GSON.toJson(entry.getValue()), new TypeToken<List<Object>>() {}.getType()));
                    break;
                case "workflows":
                    builder.withWorkFlows(GSON.fromJson(GSON.toJson(entry.getValue()), new TypeToken<List<Map<String, Object>>>() {}.getType()));
                    break;
                case "status":
                    builder.withStatus(GSON.fromJson(GSON.toJson(entry.getValue()), String.class));
                    break;
                case "dsm":
                    builder.withDsm(GSON.fromJson(GSON.toJson(entry.getValue()), new TypeToken<Map<String, Object>>() {}.getType()));
                    break;
                default:
                    break;
            }
        }
        return Optional.of(builder.build());
    }

    public List<ElasticSearchParticipantDto> parseSourceMaps(SearchHit[] searchHits) {
        if (Objects.isNull(searchHits)) return Collections.emptyList();
        List<ElasticSearchParticipantDto> result = new ArrayList<>();
        for (SearchHit searchHit: searchHits) {
            Optional<ElasticSearchParticipantDto> maybeElasticSearchResult = parseSourceMap(searchHit.getSourceAsMap());
            maybeElasticSearchResult.ifPresent(result::add);
        }
        return result;
    }

    @Override
    public ElasticSearch getParticipantsWithinRange(String esParticipantsIndex, int from, int to) {
        if (StringUtils.isBlank(esParticipantsIndex)) throw new IllegalArgumentException("ES participants index cannot be empty");
        if (to <= 0) throw new IllegalArgumentException("incorrect from/to range");
        logger.info("Collecting ES data");
        try {
            int scrollSize = to - from;
            SearchRequest searchRequest = new SearchRequest(esParticipantsIndex);
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            searchSourceBuilder.query(QueryBuilders.matchAllQuery()).sort(ElasticSearchUtil.PROFILE_CREATED_AT, SortOrder.ASC);
            searchSourceBuilder.size(scrollSize);
            searchSourceBuilder.from(from);
            searchRequest.source(searchSourceBuilder);
            SearchResponse response = ElasticSearchUtil.getClientInstance().search(searchRequest, RequestOptions.DEFAULT);
            esParticipants = parseSourceMaps(response.getHits().getHits());
            totalCount = response.getHits().getTotalHits();
        }
        catch (Exception e) {
            throw new RuntimeException("Couldn't get participants from ES for instance " + esParticipantsIndex, e);
        }
        logger.info("Got " + esParticipants.size() + " participants from ES for instance " + esParticipantsIndex);
        return this;
    }

    @Override
    public ElasticSearch getParticipantsByIds(String esParticipantsIndex, List<String> participantIds) {
        SearchRequest searchRequest = new SearchRequest(Objects.requireNonNull(esParticipantsIndex));
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(getBoolQueryOfParticipantsId(participantIds)).sort(ElasticSearchUtil.PROFILE_CREATED_AT, SortOrder.ASC);
        searchSourceBuilder.size(participantIds.size());
        searchSourceBuilder.from(0);
        searchRequest.source(searchSourceBuilder);
        SearchResponse response;
        logger.info("Collecting ES data");
        try {
            response = ElasticSearchUtil.getClientInstance().search(searchRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new RuntimeException("Couldn't get participants from ES for instance " + esParticipantsIndex, e);
        }
        esParticipants = parseSourceMaps(response.getHits().getHits());
        totalCount = response.getHits().getTotalHits();
        logger.info("Got " + esParticipants.size() + " participants from ES for instance " + esParticipantsIndex);
        return this;
    }

    @Override
    public long getParticipantsSize(String esParticipantsIndex) {
        CountRequest countRequest = new CountRequest(Objects.requireNonNull(esParticipantsIndex));
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(QueryBuilders.matchAllQuery());
        countRequest.source(searchSourceBuilder);
        CountResponse response;
        try {
            response = ElasticSearchUtil.getClientInstance().count(countRequest, RequestOptions.DEFAULT);
        } catch (IOException ioe) {
            throw new RuntimeException("Couldn't get participants size of ES for instance " + esParticipantsIndex, ioe);
        }
        return response.getCount();
    }

    @Override
    public ElasticSearch getParticipantsByRangeAndFilter(String esParticipantsIndex, int from, int to, String filter) {
        if (to <= 0) throw new IllegalArgumentException("incorrect from/to range");
        logger.info("Collecting ES data");
        List<ElasticSearch> elasticSearchList;
        try {
            int scrollSize = to - from;
            SearchRequest searchRequest = new SearchRequest(Objects.requireNonNull(esParticipantsIndex));
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            AbstractQueryBuilder<? extends AbstractQueryBuilder<?>> esQuery = ElasticSearchUtil.createESQuery(filter);
            searchSourceBuilder.query(esQuery).sort(ElasticSearchUtil.PROFILE_CREATED_AT, SortOrder.ASC);
            searchSourceBuilder.size(scrollSize);
            searchSourceBuilder.from(from);
            searchRequest.source(searchSourceBuilder);
            SearchResponse response = ElasticSearchUtil.getClientInstance().search(searchRequest, RequestOptions.DEFAULT);
            esParticipants = parseSourceMaps(response.getHits().getHits());
            totalCount = response.getHits().getTotalHits();
        }
        catch (Exception e) {
            throw new RuntimeException("Couldn't get participants from ES for instance " + esParticipantsIndex, e);
        }
        logger.info("Got " + esParticipants.size() + " participants from ES for instance " + esParticipantsIndex);
        return this;
    }

    @Override
    public ElasticSearch getParticipantsByRangeAndIds(String participantIndexES, int from, int to, List<String> participantIds) {
        SearchRequest searchRequest = new SearchRequest(Objects.requireNonNull(participantIndexES));
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(getBoolQueryOfParticipantsId(participantIds)).sort(ElasticSearchUtil.PROFILE_CREATED_AT, SortOrder.ASC);
        searchSourceBuilder.size(to - from);
        searchSourceBuilder.from(from);
        searchRequest.source(searchSourceBuilder);
        SearchResponse response;
        logger.info("Collecting ES data");
        try {
            response = ElasticSearchUtil.getClientInstance().search(searchRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new RuntimeException("Couldn't get participants from ES for instance " + participantIndexES, e);
        }
        esParticipants = parseSourceMaps(response.getHits().getHits());
        totalCount = response.getHits().getTotalHits();
        logger.info("Got " + esParticipants.size() + " participants from ES for instance " + participantIndexES);
        return this;
    }

    private BoolQueryBuilder getBoolQueryOfParticipantsId(List<String> participantIds) {
        Map<Boolean, List<String>> isGuidMap = participantIds.stream().collect(Collectors.partitioningBy(ParticipantUtil::isGuid));
        BoolQueryBuilder boolQuery = new BoolQueryBuilder();
        isGuidMap.forEach((booleanId, idValues) -> boolQuery.should(QueryBuilders.termsQuery(booleanId
                ? ElasticSearchUtil.PROFILE_GUID
                : ElasticSearchUtil.PROFILE_LEGACYALTPID, idValues)));
        return boolQuery;
    }

}
