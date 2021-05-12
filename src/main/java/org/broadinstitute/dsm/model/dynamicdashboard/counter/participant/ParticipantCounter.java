package org.broadinstitute.dsm.model.dynamicdashboard.counter.participant;

import org.broadinstitute.dsm.model.dynamicdashboard.StatisticPayload;
import org.broadinstitute.dsm.model.dynamicdashboard.StatisticResult;
import org.broadinstitute.dsm.model.dynamicdashboard.counter.Counter;
import org.broadinstitute.dsm.model.dynamicdashboard.counter.CounterResult;
import org.broadinstitute.dsm.util.SystemUtil;

public class ParticipantCounter extends Counter {

    @Override
    public StatisticResult filter(StatisticPayload statisticPayload) {
        CounterResult counterResult = new CounterResult();
        switch (statisticPayload.getFilterType()) {
            case REGISTERED:
                counterResult = new CounterResult();
                break;
            case ENROLLED:
                counterResult = new CounterResult();
                break;
        }
        return counterResult;
    }

    int filterEnrolled() {
        return 56;
    }
    int filterRegistered() {
        return 23;
    }

}
