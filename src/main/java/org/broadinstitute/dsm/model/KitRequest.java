package org.broadinstitute.dsm.model;

import lombok.Data;
import org.broadinstitute.dsm.model.ddp.DDPParticipant;

@Data
public class KitRequest {

    private String dsmKitRequestId;
    private String participantId;
    private String shortId;
    private String shippingId;
    private String externalOrderNumber;
    private DDPParticipant participant;
    private String externalOrderStatus;
    private String externalKitName;

    public KitRequest(String participantId, String shortId, DDPParticipant participant) {
        this(null, participantId, shortId, null, null, participant, null, null);
    }

    public KitRequest(String dsmKitRequestId, String participantId, String shortId, String shippingId, String externalOrderNumber, DDPParticipant participant, String externalOrderStatus, String externalKitName) {
        this.dsmKitRequestId = dsmKitRequestId;
        this.participantId = participantId;
        this.shortId = shortId;
        this.shippingId = shippingId;
        this.externalOrderNumber = externalOrderNumber;
        this.participant = participant;
        this.externalOrderStatus = externalOrderStatus;
        this.externalKitName = externalKitName;
    }
}
