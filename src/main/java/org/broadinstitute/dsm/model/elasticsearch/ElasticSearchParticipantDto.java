package org.broadinstitute.dsm.model.elasticsearch;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

public class ElasticSearchParticipantDto {

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

    public String getParticipantId() {
        return getProfile().map(esProfile -> StringUtils.isNotBlank(esProfile.getParticipantGuid())
                ? esProfile.getParticipantGuid()
                : esProfile.getParticipantLegacyAltPid())
                .orElse("");
    }

    private ElasticSearchParticipantDto(ElasticSearchParticipantDto.Builder builder) {
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

        public ElasticSearchParticipantDto build() {
            return new ElasticSearchParticipantDto(this);
        }
    }


}
