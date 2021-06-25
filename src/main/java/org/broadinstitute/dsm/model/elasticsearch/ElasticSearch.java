package org.broadinstitute.dsm.model.elasticsearch;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import lombok.Data;

@Data
public class ElasticSearch {

    private static final Gson GSON = new Gson();

    private Optional<ESAddress> address;
    private Optional<List<Object>> medicalProviders;
    private Optional<List<Object>> invitations;
    private Optional<List<Object>> activities;
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

    public static ElasticSearch parseSourceMap(Map<String, Object> sourceMap) {
        if (sourceMap == null) return new ElasticSearch.Builder().build();
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
                    builder.withActivities(GSON.fromJson(GSON.toJson(entry.getValue()), new TypeToken<List<Object>>() {}.getType()));
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
                    builder.withWorkFlows(GSON.fromJson(GSON.toJson(entry.getValue()), new TypeToken<List<Map<String, String>>>() {}.getType()));
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
        return builder.build();
    }

    public static class Builder {
        private Optional<ESAddress> address = Optional.empty();
        private Optional<List<Object>> medicalProviders = Optional.empty();
        private Optional<List<Object>> invitations = Optional.empty();
        private Optional<List<Object>> activities = Optional.empty();
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

        public Builder withActivities(List<Object> activities) {
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
