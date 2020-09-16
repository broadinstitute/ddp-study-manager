package org.broadinstitute.dsm.model;

import lombok.NonNull;
import org.broadinstitute.ddp.handlers.util.Event;
import org.broadinstitute.dsm.model.birch.DSMTestResult;

public class TestResultEvent extends Event {
    DSMTestResult eventData;

    public TestResultEvent(String eventInfo, @NonNull String eventType, @NonNull Long eventDate, DSMTestResult result) {
        super(eventInfo, eventType, eventDate);
        this.eventData = result;
    }
}
