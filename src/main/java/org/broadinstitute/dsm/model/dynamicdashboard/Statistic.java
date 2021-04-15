package org.broadinstitute.dsm.model.dynamicdashboard;

public interface Statistic {

    StatisticResult filter(StatisticPayload statisticPayload);

}
