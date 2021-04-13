package org.broadinstitute.dsm.model.dynamicdashboard.counter.participant;

import org.broadinstitute.dsm.model.dynamicdashboard.StatisticPayload;
import org.broadinstitute.dsm.model.dynamicdashboard.counter.Counter;
import org.broadinstitute.dsm.model.dynamicdashboard.counter.CounterResult;
import org.broadinstitute.dsm.util.SystemUtil;

public class ParticipantCounter implements Counter {

    @Override
    public CounterResult filter(StatisticPayload statisticPayload) {
        CounterResult counterResult = new CounterResult();
        switch (statisticPayload.getFilterType()) {
            case "REGISTERED":
                counterResult = new CounterResult(this.filterEnrolled(),
                        SystemUtil.calculatePercentage(this.filterEnrolled(), 100), "Participants Enrolled", statisticPayload.getDisplayType().toString());
                break;
        }
        return counterResult;
    }

    int filterEnrolled() {
        return 23;
    }

}
