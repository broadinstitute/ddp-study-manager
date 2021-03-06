package org.broadinstitute.dsm.db.dto.ddp.participant;

import java.util.Optional;

import lombok.Setter;

@Setter
public class ParticipantDataDto {

    private int participantDataId;
    private String ddpParticipantId;
    private int ddpInstanceId;
    private String fieldTypeId;
    private String data;
    private long lastChanged;
    private String changedBy;

    public int getParticipantDataId() {
        return participantDataId;
    }

    public Optional<String> getDdpParticipantId() {
        return Optional.ofNullable(ddpParticipantId);
    }

    public int getDdpInstanceId() {
        return ddpInstanceId;
    }

    public Optional<String> getFieldTypeId() {
        return Optional.ofNullable(fieldTypeId);
    }

    public Optional<String> getData() {
        return Optional.ofNullable(data);
    }

    public long getLastChanged() {
        return lastChanged;
    }

    public Optional<String> getChangedBy() {
        return Optional.ofNullable(changedBy);
    }

    private ParticipantDataDto(Builder builder) {
        this.participantDataId = builder.participantDataId;
        this.ddpParticipantId = builder.ddpParticipantId;
        this.ddpInstanceId = builder.ddpInstanceId;
        this.fieldTypeId = builder.fieldTypeId;
        this.data = builder.data;
        this.lastChanged = builder.lastChanged;
        this.changedBy = builder.changedBy;
    }

    public static class Builder {
        private int participantDataId;
        private String ddpParticipantId;
        private int ddpInstanceId;
        private String fieldTypeId;
        private String data;
        private long lastChanged;
        private String changedBy;
        
        public Builder withParticipantDataId(int participantDataId) {
            this.participantDataId = participantDataId;
            return this;
        }
        
        public Builder withDdpParticipantId(String ddpParticipantId) {
            this.ddpParticipantId = ddpParticipantId;
            return this;
        }

        public Builder withDdpInstanceId(int ddpInstanceId) {
            this.ddpInstanceId = ddpInstanceId;
            return this;
        }

        public Builder withFieldTypeId(String fieldTypeId) {
            this.fieldTypeId = fieldTypeId;
            return this;
        }

        public Builder withData(String data) {
            this.data = data;
            return this;
        }

        public Builder withLastChanged(long lastChanged) {
            this.lastChanged = lastChanged;
            return this;
        }

        public Builder withChangedBy(String changedBy) {
            this.changedBy = changedBy;
            return this;
        }

        public ParticipantDataDto build() {
            return new ParticipantDataDto(this);
        }
        
        
    }
}

