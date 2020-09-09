package org.broadinstitute.dsm.model;

import lombok.NonNull;
import org.broadinstitute.ddp.handlers.util.Event;
import org.broadinstitute.dsm.model.birch.TestBostonResult;

public class TestBostonEvent extends Event {
    TestBostonResult result;

    public TestBostonEvent(String eventInfo, @NonNull String eventType, @NonNull Long eventDate, TestBostonResult result) {
        super(eventInfo, eventType, eventDate);
        this.result = result;
    }
}
