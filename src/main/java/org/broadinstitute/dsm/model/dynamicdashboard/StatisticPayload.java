package org.broadinstitute.dsm.model.dynamicdashboard;

import lombok.Data;

@Data
public class StatisticPayload {

    private DisplayType displayType;
    private StatisticFor statisticFor;
    private String filterType;

    public StatisticPayload(DisplayType displayType, StatisticFor statisticFor, String filterType) {
        this.displayType = displayType;
        this.statisticFor = statisticFor;
        this.filterType = filterType;
    }
}
