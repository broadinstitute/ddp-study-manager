package org.broadinstitute.dsm.model.dynamicdashboard;

import org.broadinstitute.dsm.model.dynamicdashboard.counter.participant.ParticipantCounter;
import org.broadinstitute.dsm.model.dynamicdashboard.graph.heatmap.HeatmapGraph;

public class StatisticsCreator implements StatisticFactory {

    @Override
    public Statistic makeStatistic(StatisticPayload statisticPayload) {
        Statistic statistic = statisticPayload1 -> null;
        switch (statisticPayload.getDisplayType()) {
            case COUNTER:
                switch (statisticPayload.getStatisticFor()) {
                    case PARTICIPANT:
                        statistic = new ParticipantCounter();
                        break;
                    case MEDICAL_RECORD:
                        break;
                    default:
                        break;
                }
                break;
            case GRAPH_BAR:
                break;
            case GRAPH_HEATMAP:
                switch (statisticPayload.getStatisticFor()) {
                    case KIT:
                        statistic = new HeatmapGraph();
                    break;
                }
                break;
            default:
                break;
        }
        return statistic;
    }
}
