package org.broadinstitute.dsm.util;

import org.broadinstitute.ddp.handlers.util.Event;

public class KitEvent extends Event {
    private String eventInfo;
    private String eventType;
    private long eventDate;
    private String kitReasonType;
    private String kitRequestId;

    public KitEvent(String eventInfo, String eventType, long eventDate, String kitReasonType, String kitRequestId) {
        this.eventInfo = eventInfo;
        this.eventType = eventType;
        this.eventDate = eventDate;
        this.kitReasonType = kitReasonType;
        this.kitRequestId = kitRequestId;
    }
}
