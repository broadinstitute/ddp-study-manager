package org.broadinstitute.dsm.db.dto.ddp.kitrequest;

import lombok.Data;

@Data
public class ESSamplesDto {
    private String ddpParticipantId;
    private String kitRequestId;
    private String kitType;
    private String trackingOut;
    private String trackingIn;
    private String carrier;
    private String sent;
    private String delivered;
    private String received;

    public ESSamplesDto(String ddpParticipantId, String kitRequestId, String kitType, String trackingOut,
                        String trackingIn, String carrier, String sent, String delivered, String received) {
        this.ddpParticipantId = ddpParticipantId;
        this.kitRequestId = kitRequestId;
        this.kitType = kitType;
        this.trackingOut = trackingOut;
        this.trackingIn = trackingIn;
        this.carrier = carrier;
        this.sent = sent;
        this.delivered = delivered;
        this.received = received;
    }
}
