package org.broadinstitute.dsm.db.dto.ddp.kit;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class DDPKitRequestDto {

    private long dsmKitRequestId;
    private int ddpInstanceId;
    private String ddpKitRequestId;
    private String ddpParticipantId;
    private String createdBy;
    private long createdDate;
    private String externalOrderStatus;
    private long externalOrderDate;
    private LocalDateTime orderTransmittedAt;

    public DDPKitRequestDto(long dsmKitRequestId, int ddpInstanceId, String ddpKitRequestId, String ddpParticipantId,
                            String createdBy, long createdDate, String externalOrderStatus, long externalOrderDate,
                            LocalDateTime orderTransmittedAt) {
        this.dsmKitRequestId = dsmKitRequestId;
        this.ddpInstanceId = ddpInstanceId;
        this.ddpKitRequestId = ddpKitRequestId;
        this.ddpParticipantId = ddpParticipantId;
        this.createdBy = createdBy;
        this.createdDate = createdDate;
        this.externalOrderStatus = externalOrderStatus;
        this.externalOrderDate = externalOrderDate;
        this.orderTransmittedAt = orderTransmittedAt;
    }

    public DDPKitRequestDto(long dsmKitRequestId, int ddpInstanceId, String ddpKitRequestId, String ddpParticipantId) {
        this.dsmKitRequestId = dsmKitRequestId;
        this.ddpInstanceId = ddpInstanceId;
        this.ddpKitRequestId = ddpKitRequestId;
        this.ddpParticipantId = ddpParticipantId;
    }
}
