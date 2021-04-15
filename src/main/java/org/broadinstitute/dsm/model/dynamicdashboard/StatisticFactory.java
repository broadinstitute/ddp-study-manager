package org.broadinstitute.dsm.model.dynamicdashboard;

public interface StatisticFactory {

    Statistic makeStatistic(StatisticPayload statisticPayload);

}
