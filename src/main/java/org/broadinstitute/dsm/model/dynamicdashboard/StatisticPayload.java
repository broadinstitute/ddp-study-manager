package org.broadinstitute.dsm.model.dynamicdashboard;

import java.util.List;
import java.util.Map;

import lombok.Data;

@Data
public class StatisticPayload {

    private DisplayType displayType;
    private String displayText;
    private StatisticFor statisticFor;
    private List<Map<String, Object>> possibleValues;
    private FilterType filterType;
    private String realm;
    private int from;
    private int to;

    public StatisticPayload(DisplayType displayType, String displayText, StatisticFor statisticFor, FilterType filterType, List<Map<String, Object>> possibleValues) {
        this.displayText = displayText;
        this.displayType = displayType;
        this.statisticFor = statisticFor;
        this.filterType = filterType;
        this.possibleValues = possibleValues;
    }
}
