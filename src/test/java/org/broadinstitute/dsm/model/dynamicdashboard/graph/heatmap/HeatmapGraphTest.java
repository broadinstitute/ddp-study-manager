package org.broadinstitute.dsm.model.dynamicdashboard.graph.heatmap;

import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import org.broadinstitute.dsm.model.dynamicdashboard.DisplayType;
import org.broadinstitute.dsm.model.dynamicdashboard.StatisticPayload;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class HeatmapGraphTest {

    private static StatisticPayload statisticPayload;
    private static HeatmapGraph heatmapGraph;
    @Before
    public void setUp() throws Exception {
        List possibleValues = new Gson().fromJson(DataGeneratorUtil.possibleValuesString, List.class);
        statisticPayload = new StatisticPayload.Builder(DisplayType.GRAPH_HEATMAP, null, null)
                .withPossibleValues(possibleValues)
                .build();
        heatmapGraph = new HeatmapGraph(statisticPayload);
    }

    @Test
    public void testGetColumns() {
        List<Map<String, Object>> columns = heatmapGraph.getColumns();
        Assert.assertTrue(columns.size() > 0);
    }

    @Test
    public void testGetColorRange() {
        Map<String, Object> colorRange = heatmapGraph.getColorRange();
        Assert.assertTrue(colorRange.containsKey("color"));
    }

}