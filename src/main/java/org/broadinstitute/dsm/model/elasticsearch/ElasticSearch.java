package org.broadinstitute.dsm.model.elasticsearch;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import lombok.Data;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.dsm.util.ElasticSearchUtil;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Setter
public class ElasticSearch implements ElasticSearchable {

    private static final Logger logger = LoggerFactory.getLogger(ElasticSearch.class);
    private static final Gson GSON = new Gson();

    private ESAddress address;
    private List<Object> medicalProviders;
    private List<Object> invitations;
    private List<ESActivities> activities;
    private Long statusTimeStamp;
    private ESProfile profile;
    private List<Object> files;
    private List<String> proxies;
    private List<Map<String, String>> workflows;
    private String status;
    private Map<String, Object> dsm;

    public Optional<ESAddress> getAddress() {
        return Optional.ofNullable(address);
    }

    public Optional<List<Object>> getMedicalProviders() {
        return Optional.ofNullable(medicalProviders);
    }

    public Optional<List<Object>> getInvitations() {
        return Optional.ofNullable(invitations);
    }

    public Optional<List<ESActivities>> getActivities() {
        return Optional.ofNullable(activities);
    }

    public Optional<Long> getStatusTimeStamp() {
        return Optional.ofNullable(statusTimeStamp);
    }

    public Optional<ESProfile> getProfile() {
        return Optional.ofNullable(profile);
    }

    public Optional<List<Object>> getFiles() {
        return Optional.ofNullable(files);
    }

    public Optional<List<String>> getProxies() {
        return Optional.ofNullable(proxies);
    }

    public Optional<List<Map<String, String>>> getWorkflows() {
        return Optional.ofNullable(workflows);
    }

    public Optional<String> getStatus() {
        return Optional.ofNullable(status);
    }

    public Optional<Map<String, Object>> getDsm() {
        return Optional.ofNullable(dsm);
    }

    private ElasticSearch(Builder builder) {
        this.address = builder.address;
        this.medicalProviders = builder.medicalProviders;
        this.invitations = builder.invitations;
        this.activities = builder.activities;
        this.statusTimeStamp = builder.statusTimeStamp;
        this.profile = builder.profile;
        this.files = builder.files;
        this.proxies = builder.proxies;
        this.workflows = builder.workflows;
        this.status = builder.status;
        this.dsm = builder.dsm;
    }

    public static Optional<ElasticSearch> parseSourceMap(Map<String, Object> sourceMap) {
        if (sourceMap == null) return Optional.of(new ElasticSearch.Builder().build());
        Builder builder = new Builder();
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

    public List<ElasticSearch> parseSourceMaps(SearchHit[] searchHits) {
        if (Objects.isNull(searchHits)) return Collections.emptyList();
        List<ElasticSearch> result = new ArrayList<>();
        for (SearchHit searchHit: searchHits) {
            Optional<ElasticSearch> maybeElasticSearchResult = parseSourceMap(searchHit.getSourceAsMap());
            maybeElasticSearchResult.ifPresent(result::add);
        }
        return result;
    }

    @Override
    public List<ElasticSearch> getParticipantsWithinRange(String esParticipantsIndex, int from, int to) {
        if (StringUtils.isBlank(esParticipantsIndex)) throw new IllegalArgumentException("ES participants index cannot be empty");
        if (to <= 0) throw new IllegalArgumentException("incorrect from/to range");
        logger.info("Collecting ES data");
        List<ElasticSearch> elasticSearchList;
        try {
            int scrollSize = to - from;
            SearchRequest searchRequest = new SearchRequest(esParticipantsIndex);
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            searchSourceBuilder.query(QueryBuilders.matchAllQuery()).sort(ElasticSearchUtil.PROFILE_CREATED_AT, SortOrder.ASC);
            searchSourceBuilder.size(scrollSize);
            searchSourceBuilder.from(from);
            searchRequest.source(searchSourceBuilder);
            SearchResponse response = ElasticSearchUtil.getClientInstance().search(searchRequest, RequestOptions.DEFAULT);
            elasticSearchList = parseSourceMaps(response.getHits().getHits());
        }
        catch (Exception e) {
            throw new RuntimeException("Couldn't get participants from ES for instance " + esParticipantsIndex, e);
        }
        logger.info("Got " + elasticSearchList.size() + " participants from ES for instance " + esParticipantsIndex);
        return elasticSearchList;
    }

    @Override
    public List<ElasticSearch> getParticipantsByIds(String esParticipantsIndex, List<String> participantIds) {
        SearchRequest searchRequest = new SearchRequest(esParticipantsIndex);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        TermQueryBuilder participantIdsQuery = QueryBuilders.termQuery("participantId", participantIds);
        searchSourceBuilder.query(participantIdsQuery).sort(ElasticSearchUtil.PROFILE_CREATED_AT, SortOrder.ASC);
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
        List<ElasticSearch> elasticSearchList = parseSourceMaps(response.getHits().getHits());
        logger.info("Got " + elasticSearchList.size() + " participants from ES for instance " + esParticipantsIndex);
        return elasticSearchList;
    }

    public String getParticipantIdFromProfile() {
        return getProfile().map(esProfile -> StringUtils.isNotBlank(esProfile.getParticipantGuid())
                ? esProfile.getParticipantGuid()
                : esProfile.getParticipantLegacyAltPid())
                .orElse("");
    }

    public static class Builder {
        private ESAddress address;
        private List<Object> medicalProviders;
        private List<Object> invitations;
        private List<ESActivities> activities;
        private Long statusTimeStamp;
        private ESProfile profile;
        private List<Object> files;
        private List<String> proxies;
        private List<Map<String, String>> workflows;
        private String status;
        private Map<String, Object> dsm;

        public Builder() {}

        public Builder withAddress(ESAddress esAddress) {
            this.address = esAddress;
            return this;
        }

        public Builder withMedicalProviders(List<Object> medicalProviders) {
            this.medicalProviders = medicalProviders;
            return this;
        }

        public Builder withInvitations(List<Object> invitations) {
            this.invitations = invitations;
            return this;
        }

        public Builder withActivities(List<ESActivities> activities) {
            this.activities = activities;
            return this;
        }

        public Builder withStatusTimeStamp(Long statusTimeStamp) {
            this.statusTimeStamp = statusTimeStamp;
            return this;
        }

        public Builder withProfile(ESProfile profile) {
            this.profile = profile;
            return this;
        }

        public Builder withFiles(List<Object> files) {
            this.files = files;
            return this;
        }

        public Builder withProxies(List<String> proxies) {
            this.proxies = proxies;
            return this;
        }

        public Builder withWorkFlows(List<Map<String, String>> workflows) {
            this.workflows = workflows;
            return this;
        }

        public Builder withStatus(String status) {
            this.status = status;
            return this;
        }

        public Builder withDsm(Map<String, Object> dsm) {
            this.dsm = dsm;
            return this;
        }

        public ElasticSearch build() {
            return new ElasticSearch(this);
        }
    }

}
