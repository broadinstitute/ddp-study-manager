package org.broadinstitute.dsm.model.dynamicdashboard;

import com.google.gson.Gson;
import org.junit.Assert;
import org.junit.Test;

public class AddStatisticPayloadTest {


    @Test
    public void testAddStatisticPayloadConvertion() {
        String json = "{displayType: \"GRAPH_BAR\", statisticFor: \"PARTICIPANT\", filterType: \"ENROLLED\"}";
        StatisticPayload addStatisticPayload = new Gson().fromJson(json, StatisticPayload.class);
        Assert.assertEquals("GRAPH_BAR", addStatisticPayload.getDisplayType().toString());
        Assert.assertEquals("PARTICIPANT", addStatisticPayload.getStatisticFor().toString());
        Assert.assertEquals("ENROLLED", addStatisticPayload.getFilterType().toString());
    }

}