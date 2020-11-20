package org.broadinstitute.dsm.model.ups;

import java.text.SimpleDateFormat;
import java.time.Instant;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class UPSActivityTest {

    private UPSActivity inTransitActivity;

    private UPSActivity deliveredActivity;

    @Before
    public void setUp() {
        inTransitActivity = new UPSActivity(new UPSStatus("Coming Back","On the way!","I"), "20201113", "195327");
        deliveredActivity = new UPSActivity(new UPSStatus("Done","You got it!","D"), "20201117", "015327");
    }


    @Test
    public void testDateParsing() throws Exception {
        String dateString = inTransitActivity.getDate();
        String timeString = inTransitActivity.getTime();
        Instant expectedDate = new SimpleDateFormat("yyyyMMdd kkmmss").parse(dateString + " " + timeString).toInstant();

        Assert.assertEquals(dateString + " " + timeString, inTransitActivity.getDateTimeString());
        Assert.assertEquals(expectedDate, inTransitActivity.getInstant());
    }

    @Test
    public void testInTransit() {
        Assert.assertTrue(inTransitActivity.isInTransit());
        Assert.assertFalse(inTransitActivity.isDelivered());
    }

    @Test
    public void testDelivered() {
        Assert.assertFalse(deliveredActivity.isInTransit());
        Assert.assertTrue(deliveredActivity.isDelivered());
    }
}
