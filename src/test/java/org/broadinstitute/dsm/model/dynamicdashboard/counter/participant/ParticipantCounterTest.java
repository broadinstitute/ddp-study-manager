package org.broadinstitute.dsm.model.dynamicdashboard.counter.participant;

import com.google.gson.Gson;
import org.broadinstitute.dsm.model.dynamicdashboard.StatisticPayload;
import org.broadinstitute.dsm.model.dynamicdashboard.counter.CounterResult;
import org.broadinstitute.dsm.model.dynamicdashboard.counter.participant.ParticipantCounter;
import org.broadinstitute.dsm.model.dynamicdashboard.counter.participant.ParticipantCounterFilterType;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class ParticipantCounterTest {

    private static Gson gson;

    @BeforeClass
    public static void beforeStart() {
        gson = new Gson();
    }


    @Test
    public void testTotalEnrolledFilter() {
        String payload = "{displayType:\"COUNTER\", statisticFor: \"PARTICIPANT\", filterType: \"ENROLLED\"}";
        StatisticPayload statisticPayload = gson.fromJson(payload, StatisticPayload.class);
        ParticipantCounter participantCounter = new ParticipantCounter();
        CounterResult counterResult = participantCounter.filter(statisticPayload);
        Assert.assertEquals(participantCounter.filterEnrolled(), counterResult.getTotalItems());
    }
//    @Test
//    public void testEnrolledPercentage() {
//        Counter counter = new ParticipantCounter();
//        CounterResult counterResult = counter.filter(ParticipantCounterFilterType.ENROLLED);
//        Assert.assertEquals(46, counterResult.getPercentage());
//    }

}