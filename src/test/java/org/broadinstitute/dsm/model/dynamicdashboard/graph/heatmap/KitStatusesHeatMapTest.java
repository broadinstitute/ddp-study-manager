package org.broadinstitute.dsm.model.dynamicdashboard.graph.heatmap;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import org.broadinstitute.dsm.db.dto.ddp.kit.DDPKitRequestDto;
import org.broadinstitute.dsm.model.dynamicdashboard.DisplayType;
import org.broadinstitute.dsm.model.dynamicdashboard.StatisticPayload;
import org.broadinstitute.dsm.util.ElasticSearchUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class KitStatusesHeatMapTest {

    private static KitStatusesHeatMap kitStatusesHeatMap;
    private static Map<String, Map<String, Object>> participantESData;
    private static StatisticPayload statisticPayload;
    private static final String HRUID = "5HBIKL";
    private static final String GUID = "NKNI6A8C2UM5L23XKSKY";
    private static String activityCode;
    private static String activityStatus;
    private static long activityCompletedAt;

    @Before
    public void setUp() throws Exception {
        activityCode = "CONSENT";
        activityStatus = "COMPLETE";
        activityCompletedAt = Instant.now().toEpochMilli();
        Object profileData = Map.of(
                ElasticSearchUtil.FIRST_NAME_FIELD, "Mickey",
                ElasticSearchUtil.LAST_NAME_FIELD, "Mouse",
                ElasticSearchUtil.HRUID, HRUID,
                ElasticSearchUtil.GUID, GUID
        );
        Object activity = Map.of(
                ElasticSearchUtil.ACTIVITY_CODE, KitStatusesHeatMapTest.activityCode,
                ElasticSearchUtil.STATUS, activityStatus,
                ElasticSearchUtil.COMPLETED_AT, activityCompletedAt
        );
        Map<String, Object> participantData = Map.of(
                ElasticSearchUtil.PROFILE, profileData,
                ElasticSearchUtil.ACTIVITIES, Collections.singletonList(activity)
        );
        participantESData = Map.of(GUID, participantData);
        List possibleValues = new Gson().fromJson(DataGeneratorUtil.possibleValuesString, List.class);
        statisticPayload = new StatisticPayload.Builder(DisplayType.GRAPH_HEATMAP, null, null)
                .withPossibleValues(possibleValues)
                .build();
        kitStatusesHeatMap = new KitStatusesHeatMap(statisticPayload);
    }

    @Test
    public void testGetParticipantsGuidsWithShortIds() {
        Map<String, String> participantsGuidsWithShortIds = kitStatusesHeatMap.getParticipantsGuidsWithShortIds(participantESData);
        Assert.assertEquals(HRUID, participantsGuidsWithShortIds.get(GUID));
    }

    @Test
    public void testGetConsentCompletedAtOfParticipant() {
        Optional<Long> maybeConsentCompletedAtOfParticipant = kitStatusesHeatMap.getConsentCompletedAtOfParticipant(participantESData.get(GUID));
        Assert.assertEquals(activityCompletedAt, (long) maybeConsentCompletedAtOfParticipant.orElse(0L));
    }

    @Test
    public void testGetParticipantsWithKitsSeparatedByMonth() {
        DDPKitRequestDto firstKit = DDPKitRequestDto.builder()
                .ddpParticipantId(GUID)
                .externalOrderStatus("SHIPPED")
                .externalOrderDate(activityCompletedAt + 5000)
                .build();
        DDPKitRequestDto secondtKit = DDPKitRequestDto.builder()
                .ddpParticipantId(GUID)
                .externalOrderStatus("SHIPPED")
                .externalOrderDate(Instant.ofEpochMilli(activityCompletedAt).plus(31, ChronoUnit.DAYS).toEpochMilli())
                .build();
        DDPKitRequestDto thirdtKit = DDPKitRequestDto.builder()
                .ddpParticipantId(GUID)
                .externalOrderStatus("SHIPPED")
                .externalOrderDate(Instant.ofEpochMilli(activityCompletedAt).plus(61, ChronoUnit.DAYS).toEpochMilli())
                .build();
        Map<String, Map<String, List<Object>>> participantsWithKitsSeparatedByMonth = kitStatusesHeatMap
                .getParticipantsWithKitsSeparatedByMonth(participantESData, Map.of(HRUID, Arrays.asList(firstKit, secondtKit, thirdtKit)));
        DDPKitRequestDto firstDdpKitRequestDto = (DDPKitRequestDto) participantsWithKitsSeparatedByMonth.get(HRUID).get("1ST").get(0);
        DDPKitRequestDto secondDdpKitRequestDto = (DDPKitRequestDto) participantsWithKitsSeparatedByMonth.get(HRUID).get("2ND").get(0);
        DDPKitRequestDto thirdDdpKitRequestDto = (DDPKitRequestDto) participantsWithKitsSeparatedByMonth.get(HRUID).get("3RD").get(0);
        Assert.assertEquals(firstKit, firstDdpKitRequestDto);
        Assert.assertEquals(secondtKit, secondDdpKitRequestDto);
        Assert.assertEquals(thirdtKit, thirdDdpKitRequestDto);
    }

    @Test
    public void testGetHeatmapRows() {
        DDPKitRequestDto ddpKitRequestDto = DDPKitRequestDto.builder()
                .ddpParticipantId(GUID)
                .externalOrderStatus("SHIPPED")
                .externalOrderDate(activityCompletedAt + 5000)
                .build();
        Map<String, Map<String, List<Object>>> participantsWithKitsSeparatedByMonth = kitStatusesHeatMap
                .getParticipantsWithKitsSeparatedByMonth(participantESData, Map.of(HRUID, Collections.singletonList(ddpKitRequestDto)));
        List<HeatmapGraphData.HeatMapRow> heatmapRows =
                kitStatusesHeatMap.getHeatmapRows(participantESData, participantsWithKitsSeparatedByMonth);
        Assert.assertTrue(heatmapRows.stream()
                .map(HeatmapGraphData.HeatMapRow::getId)
                .collect(Collectors.toList()).contains(HRUID));
    }

    @Test
    public void testGetColorRange() {
        String colorRange;
        colorRange = kitStatusesHeatMap.getKitStatus("D", false, LocalDateTime.now());
        Assert.assertEquals("Returned", colorRange);
        colorRange = kitStatusesHeatMap.getKitStatus("M", false, LocalDateTime.now());
        Assert.assertEquals("In transit (incoming)", colorRange);
        colorRange = kitStatusesHeatMap.getKitStatus("I", false, LocalDateTime.now().minus(8, ChronoUnit.DAYS));
        Assert.assertEquals("Not Returned", colorRange);
        colorRange = kitStatusesHeatMap.getKitStatus("M", true, LocalDateTime.now());
        Assert.assertEquals("In transit (outgoing)", colorRange);
        colorRange = kitStatusesHeatMap.getKitStatus("I", true, "20210608 165925");
        Assert.assertEquals("Not Delivered", colorRange);
    }
}