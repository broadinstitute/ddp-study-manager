package org.broadinstitute.dsm.model.dynamicdashboard.counter;

import org.broadinstitute.dsm.model.dynamicdashboard.StatisticResult;
import lombok.Data;

@Data
public class CounterResult extends StatisticResult {

    private int totalItems;
    private double percentage;
    private String displayText;
    private String displayType;

    public CounterResult() {};

    public CounterResult(int totalItems, double percentage, String displayText, String displayType) {
        this.totalItems = totalItems;
        this.percentage = percentage;
        this.displayText = displayText;
        this.displayType = displayType;
    }

}
