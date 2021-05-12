package org.broadinstitute.dsm.model.dynamicdashboard.graph;

import org.broadinstitute.dsm.model.dynamicdashboard.DisplayType;
import org.broadinstitute.dsm.model.dynamicdashboard.StatisticResult;

public abstract class GraphResult extends StatisticResult {

    protected String displayText;
    protected DisplayType displayType;

    public GraphResult() {}

    protected GraphResult(String displayText, DisplayType displayType) {
        this.displayText = displayText;
        this.displayType = displayType;
    }
}
