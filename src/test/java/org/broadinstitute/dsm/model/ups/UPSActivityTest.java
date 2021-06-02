package org.broadinstitute.dsm.model.ups;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.google.gson.Gson;
import org.broadinstitute.dsm.db.dto.ups.UPSActivityDto;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class UPSActivityTest {

    private static final String UPS_LOCATION_JSON = "{\"address\":{\"city\":\"Tbilisi\",\"stateProvince\":\"\",\"postalCode\":\"\"}}";

    private UPSActivity inTransitActivity;

    private UPSActivity deliveredActivity;

    private UPSActivity labelGenerated = new UPSActivity((String) null, new UPSStatus("M", "Printed some stuff", "MO"), "20201117", "015327", null, null, null);

    @Before
    public void setUp() {
        inTransitActivity = new UPSActivity((UPSLocation) null, new UPSStatus("I", "On the way!", "IX"), "20201113", "195327", null, null, null);
        deliveredActivity = new UPSActivity((UPSLocation) null, new UPSStatus("D", "You got it!", "DX"), "20201117", "015327", null, null, null);
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
        Assert.assertTrue(inTransitActivity.isOnItsWay());
    }

    @Test
    public void testDelivered() {
        Assert.assertTrue(deliveredActivity.isOnItsWay());
    }

    @Test
    public void testLabelGeneratedIsNotOnItsWay() {
        Assert.assertFalse(labelGenerated.isOnItsWay());
    }

    @Test
    public void testConvertJsonToUpsLocation() {
        UPSLocation upsLocation = new UPSActivity().convertJsonToUpsLocation(UPSActivityTest.UPS_LOCATION_JSON);
        String upsLocationJsonFromObject = new Gson().toJson(upsLocation);
        Assert.assertEquals(UPSActivityTest.UPS_LOCATION_JSON, upsLocationJsonFromObject);
    }

    @Test
    public void testProcessActivities() {
        List<UPSActivityDto> upsActivityDtos = upsActivityDtosGenerator(20);
        List<UPSActivity> upsActivities = new UPSActivity().processActivities(upsActivityDtos);
        Map<String, String> address = (Map<String, String>) new Gson().fromJson(UPS_LOCATION_JSON, Map.class).get("address");
        String city = address.get("city");
        Assert.assertEquals(upsActivityDtos.size(), upsActivities.size());
        Assert.assertEquals(city, upsActivities.get(0).getLocation().getAddress().getCity());
    }


    private List<UPSActivityDto> upsActivityDtosGenerator(int quantity) {
        List<UPSActivityDto> upsActivityDtos = new ArrayList<>();
        List<String> statusTypes = Arrays.asList("M", "I", "D");
        List<String> statusCodes = Arrays.asList("MP ", "OR", "DP", "AR");
        Random rand = new Random();
        for (int i = 0; i < quantity; i++) {
            upsActivityDtos.add(
                UPSActivityDto.builder()
                        .upsActvityId(Math.abs(rand.nextInt()))
                        .upsPackageId(Math.abs(rand.nextInt()))
                        .upsActivityDateTime(LocalDateTime.now())
                        .upsStatusType(statusTypes.get(rand.nextInt(statusTypes.size())))
                        .upsStatusDescription("")
                        .upsStatusCode(statusCodes.get(rand.nextInt(statusCodes.size())))
                        .upsLocation(UPS_LOCATION_JSON)
                        .build()
            );
        }
        return upsActivityDtos;
    }

}
