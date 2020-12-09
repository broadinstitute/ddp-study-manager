package org.broadinstitute.dsm.util;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.ddp.handlers.util.Event;

public class KitEvent extends Event {
    private String eventInfo;
    private String eventType;
    private long eventDate;
    private String kitReasonType;
    private String kitRequestId;

    public String NORMAL = "NORMAL";

    public KitEvent(String eventInfo, String eventType, long eventDate, String kitReasonType, String kitRequestId) {
        this.eventInfo = eventInfo;
        this.eventType = eventType;
        this.eventDate = eventDate;
        if (StringUtils.isBlank(kitReasonType)) {
            kitReasonType = NORMAL;
        }
        this.kitReasonType = kitReasonType;
        this.kitRequestId = kitRequestId;
    }
}
