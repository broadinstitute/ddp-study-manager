package org.broadinstitute.dsm.model.dynamicdashboard.graph.heatmap;

import java.util.List;
import java.util.Map;

import org.broadinstitute.dsm.model.dynamicdashboard.StatisticPayload;
import org.broadinstitute.dsm.model.dynamicdashboard.StatisticResult;
import org.broadinstitute.dsm.model.dynamicdashboard.graph.Graph;

public class HeatmapGraph extends Graph {

    private StatisticPayload statisticPayload;

    public HeatmapGraph() {}

    public HeatmapGraph(StatisticPayload statisticPayload) {
        this.statisticPayload = statisticPayload;
    }

    @Override
    public StatisticResult filter(StatisticPayload statisticPayload) {
        HeatmapGraphData heatmapResult = null;
        switch (statisticPayload.getFilterType()) {
            case KIT_STATUSES:
                heatmapResult = new KitStatusesHeatMap(statisticPayload).samplesByStatuses();
                break;

        }
        return heatmapResult;
    }

    protected StatisticPayload getStatisticPayload() {
        return this.statisticPayload;
    }

    protected List<Map<String, Object>> getColumns() {
        return (List<Map<String, Object>>) statisticPayload.getPossibleValues().stream()
                .filter(pv -> pv.containsKey("columns"))
                .findAny()
                .orElse(Map.of())
                .get("columns");
    }

    protected Map<String, Object> getColorRange() {
        return (Map<String, Object>) statisticPayload.getPossibleValues().stream()
                .filter(pv -> pv.containsKey("colorrange"))
                .findAny()
                .orElse(Map.of())
                .get("colorrange");
    }
}
