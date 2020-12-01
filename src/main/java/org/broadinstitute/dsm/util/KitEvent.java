package org.broadinstitute.dsm.util;

import lombok.NonNull;
import org.broadinstitute.ddp.handlers.util.Event;

public class KitEvent extends Event {
    private String kitUploadType;
    private String ddpKitRequestId;
    public KitEvent( String eventInfo,  String eventType, long eventDate, String kitUploadType, String ddpKitRequestId){
        super ( eventInfo,eventType, eventDate);
        this.kitUploadType = kitUploadType;
        this.ddpKitRequestId = ddpKitRequestId;
    }
}
