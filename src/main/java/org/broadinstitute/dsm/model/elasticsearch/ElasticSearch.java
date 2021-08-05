package org.broadinstitute.dsm.model.elasticsearch;

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
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.dsm.util.ElasticSearchUtil;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Data
public class ElasticSearch implements ElasticSearchable {

    private static final Logger logger = LoggerFactory.getLogger(ElasticSearch.class);
    private static final Gson GSON = new Gson();

    private Optional<ESAddress> address;
    private Optional<List<Object>> medicalProviders;
    private Optional<List<Object>> invitations;
    private Optional<List<ESActivities>> activities;
    private Optional<Long> statusTimeStamp;
    private Optional<ESProfile> profile;
    private Optional<List<Object>> files;
    private Optional<List<Object>> proxies;
    private Optional<List<Map<String, String>>> workflows;
    private Optional<String> status;
    private Optional<Map<String, Object>> dsm;

    private ElasticSearch(Builder builder) {
        this.address = builder.address;
        this.medicalProviders = builder.medicalProviders;
        this.invitations = builder.invitations;
        this.activities = builder.activities;
        this.statusTimeStamp = builder.statusTimeStamp;
        this.profile = builder.profile;
        this.files = builder.files;
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

    public static class Builder {
        private Optional<ESAddress> address = Optional.empty();
        private Optional<List<Object>> medicalProviders = Optional.empty();
        private Optional<List<Object>> invitations = Optional.empty();
        private Optional<List<ESActivities>> activities = Optional.empty();
        private Optional<Long> statusTimeStamp = Optional.empty();
        private Optional<ESProfile> profile = Optional.empty();
        private Optional<List<Object>> files = Optional.empty();
        private Optional<List<Object>> proxies = Optional.empty();
        private Optional<List<Map<String, String>>> workflows = Optional.empty();
        private Optional<String> status = Optional.empty();
        private Optional<Map<String, Object>> dsm = Optional.empty();

        public Builder() {}

        public Builder withAddress(ESAddress esAddress) {
            this.address = Optional.ofNullable(esAddress);
            return this;
        }

        public Builder withMedicalProviders(List<Object> medicalProviders) {
            this.medicalProviders = Optional.ofNullable(medicalProviders);
            return this;
        }

        public Builder withInvitations(List<Object> invitations) {
            this.invitations = Optional.ofNullable(invitations);
            return this;
        }

        public Builder withActivities(List<ESActivities> activities) {
            this.activities = Optional.ofNullable(activities);
            return this;
        }

        public Builder withStatusTimeStamp(Long statusTimeStamp) {
            this.statusTimeStamp = Optional.ofNullable(statusTimeStamp);
            return this;
        }

        public Builder withProfile(ESProfile profile) {
            this.profile = Optional.ofNullable(profile);
            return this;
        }

        public Builder withFiles(List<Object> files) {
            this.files = Optional.ofNullable(files);
            return this;
        }

        public Builder withProxies(List<Object> proxies) {
            this.proxies = Optional.ofNullable(proxies);
            return this;
        }

        public Builder withWorkFlows(List<Map<String, String>> workflows) {
            this.workflows = Optional.ofNullable(workflows);
            return this;
        }

        public Builder withStatus(String status) {
            this.status = Optional.ofNullable(status);
            return this;
        }

        public Builder withDsm(Map<String, Object> dsm) {
            this.dsm = Optional.ofNullable(dsm);
            return this;
        }

        public ElasticSearch build() {
            return new ElasticSearch(this);
        }
    }

}
