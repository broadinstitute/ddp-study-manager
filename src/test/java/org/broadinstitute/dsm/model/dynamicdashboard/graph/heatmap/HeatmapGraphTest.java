package org.broadinstitute.dsm.model.dynamicdashboard.graph.heatmap;

import com.google.gson.Gson;
import org.junit.Assert;
import org.junit.Test;

public class HeatmapGraphTest {


    @Test
    public void test() {
        String json = "[{\"name\": \"Kit\", \"series\": [{\"name\": \"Delivered\", \"value\": \"1000\"}]}]";
        HeatmapGraphData[] heatmapGraphResult = new Gson().fromJson(json, HeatmapGraphData[].class);
        Assert.assertTrue(heatmapGraphResult != null);
    }

}