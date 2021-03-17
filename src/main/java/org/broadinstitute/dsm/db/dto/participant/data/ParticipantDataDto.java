package org.broadinstitute.dsm.db.dto.participant.data;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ParticipantDataDto {

    private long participantDataId;
    private String ddpParticipantId;
    private long ddpInstanceId;
    private String fieldTypeId;
    private String data;

    public ParticipantDataDto(long participantDataId, String ddpParticipantId, long ddpInstanceId, String fieldTypeId, String data) {
        this.participantDataId = participantDataId;
        this.ddpParticipantId = ddpParticipantId;
        this.ddpInstanceId = ddpInstanceId;
        this.fieldTypeId = fieldTypeId;
        this.data = data;
    }
}

