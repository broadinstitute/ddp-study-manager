package org.broadinstitute.dsm.model.dynamicdashboard;

import lombok.Data;

@Data
public class StatisticPayload {

    private DisplayType displayType;
    private StatisticFor statisticFor;
    private FilterType filterType;

    public StatisticPayload(DisplayType displayType, StatisticFor statisticFor, FilterType filterType) {
        this.displayType = displayType;
        this.statisticFor = statisticFor;
        this.filterType = filterType;
    }
}
