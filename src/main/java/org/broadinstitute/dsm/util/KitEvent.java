package org.broadinstitute.dsm.util;

import org.broadinstitute.ddp.handlers.util.Event;

public class KitEvent extends Event {
    private String eventInfo;
    private String eventType;
    private long eventDate;
    private String kitUploadType;
    private String ddpKitRequestId;

    public KitEvent(String eventInfo, String eventType, long eventDate, String kitUploadType, String ddpKitRequestId) {
        this.eventInfo = eventInfo;
        this.eventType = eventType;
        this.eventDate = eventDate;
        this.kitUploadType = kitUploadType;
        this.ddpKitRequestId = ddpKitRequestId;
    }
}
