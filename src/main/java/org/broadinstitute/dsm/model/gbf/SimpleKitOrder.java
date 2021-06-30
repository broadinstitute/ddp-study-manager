package org.broadinstitute.dsm.model.gbf;

import java.time.Instant;

public class SimpleKitOrder {

    private final Address recipientAddress;

    private final String externalKitOrderNumber;

    private final String externalKitName;

    private final String participantGuid;

    private final String shortId;

    private final Instant scheduledAt;

    public SimpleKitOrder(Address recipientAddress, String externalKitOrderNumber, String externalKitName, String participantGuid, String shortId, Instant scheduledAt) {
        this.recipientAddress = recipientAddress;
        this.externalKitOrderNumber = externalKitOrderNumber;
        this.externalKitName = externalKitName;
        this.participantGuid = participantGuid;
        this.shortId = shortId;
        this.scheduledAt = scheduledAt;
    }

    public Address getRecipientAddress() {
        return recipientAddress;
    }

    public String getExternalKitOrderNumber() {
        return externalKitOrderNumber;
    }

    public String getExternalKitName() {
        return externalKitName;
    }

    public String getParticipantGuid() {
        return participantGuid;
    }

    public String getShortId() {
        return shortId;
    }

    public Instant getScheduledAt() {
        return scheduledAt;
    }
}
