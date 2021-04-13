package org.broadinstitute.dsm.model.dynamicdashboard.counter;

import org.broadinstitute.dsm.model.dynamicdashboard.StatisticPayload;

public interface Counter {

    CounterResult filter(StatisticPayload statisticPayload);

}
