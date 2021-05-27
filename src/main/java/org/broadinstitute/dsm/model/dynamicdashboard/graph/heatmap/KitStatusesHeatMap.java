package org.broadinstitute.dsm.model.dynamicdashboard.graph.heatmap;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.dsm.db.DDPInstance;
import org.broadinstitute.dsm.db.Participant;
import org.broadinstitute.dsm.db.dao.ddp.instance.DDPInstanceDao;
import org.broadinstitute.dsm.db.dao.ddp.kit.DDPKitDao;
import org.broadinstitute.dsm.db.dao.ddp.kit.DDPKitRequestDao;
import org.broadinstitute.dsm.db.dao.ups.UPSActivityDao;
import org.broadinstitute.dsm.db.dao.ups.UPSPackageDao;
import org.broadinstitute.dsm.db.dao.ups.UPShipmentDao;
import org.broadinstitute.dsm.db.dto.ddp.kit.DDPKitDto;
import org.broadinstitute.dsm.db.dto.ddp.kit.DDPKitRequestDto;
import org.broadinstitute.dsm.db.dto.ups.UPSActivityDto;
import org.broadinstitute.dsm.db.dto.ups.UPSPackageDto;
import org.broadinstitute.dsm.db.dto.ups.UPShipmentDto;
import org.broadinstitute.dsm.model.dynamicdashboard.StatisticPayload;
import org.broadinstitute.dsm.util.ElasticSearchUtil;

public class KitStatusesHeatMap extends HeatmapGraph {

    static DateTimeFormatter dtf = new DateTimeFormatterBuilder().appendPattern("yyyyMMdd HHmmss").toFormatter();

    public KitStatusesHeatMap(StatisticPayload statisticPayload) {
        super(statisticPayload);
    }

    public HeatmapGraphData samplesByStatuses() {
        DDPInstance ddpInstanceByRealm = DDPInstanceDao.getDDPInstanceByRealm(getStatisticPayload().getRealm());

        Map<String, Map<String, Object>> ddpParticipantsFromES =
                ElasticSearchUtil.getDDPParticipantsFromESWithRange(ddpInstanceByRealm.getName(), ddpInstanceByRealm.getParticipantIndexES(),
                        getStatisticPayload().getFrom(), getStatisticPayload().getTo(), getStatisticPayload().getSortOrder());

        Map<String, String> participantsGuidsWithShortIds = getParticipantsGuidsWithShortIds(ddpParticipantsFromES);

        Map<String, List<DDPKitRequestDto>> participantsWithKits = getParticipantsWithKitsCreatedBySystem(participantsGuidsWithShortIds);

        Map<String, Map<String, List<Object>>> participantsWithKitsSeparatedByMonth =
                getParticipantsWithKitsSeparatedByMonth(ddpParticipantsFromES, participantsWithKits);

        List<HeatmapGraphData.HeatMapRow> heatmapGraphRows = getHeatmapRows(ddpParticipantsFromES, participantsWithKitsSeparatedByMonth);

        List<HeatmapGraphData.HeatMapDataSet> heatMapDataSet = getHeatmapDataset(participantsWithKitsSeparatedByMonth);

        List<Map<String, Object>> columns = getColumns();
        Map<String, Object> colorRange = getColorRange();
        return new HeatmapGraphData.Builder(heatmapGraphRows, columns, colorRange, heatMapDataSet)
                .withDisplayType(getStatisticPayload().getDisplayType())
                .withDisplayText(getStatisticPayload().getDisplayText())
                .withDashboardSettingId(getStatisticPayload().getDashboardSettingId())
                .build();
    }

    List<HeatmapGraphData.HeatMapDataSet> getHeatmapDataset(Map<String, Map<String, List<Object>>> participantsWithKitsSeparatedByMonth) {
        List<HeatmapGraphData.HeatMapDataSet> heatMapDataSet = new ArrayList<>();
        participantsWithKitsSeparatedByMonth.forEach(
                (pId, value) -> value.forEach((k, v) -> heatMapDataSet.add(new HeatmapGraphData.HeatMapDataSet(pId, k, getColorRange(v)))));
        return heatMapDataSet;
    }

    List<HeatmapGraphData.HeatMapRow> getHeatmapRows(Map<String, Map<String, Object>> ddpParticipantsFromES,
                                                         Map<String, Map<String, List<Object>>> participantsWithKitsSeparatedByMonth) {
        return participantsWithKitsSeparatedByMonth.keySet().stream()
                .map(shortId -> {
                    String participantGuid =
                            Participant.getParticipantGuidByHruidFromParticipantESData(ddpParticipantsFromES, shortId);
                    Map<String, String> profile = (Map<String, String>) ddpParticipantsFromES.get(participantGuid).get(ElasticSearchUtil.PROFILE);
                    String firstName = profile.get(ElasticSearchUtil.FIRST_NAME_FIELD);
                    String lastName = profile.get(ElasticSearchUtil.LAST_NAME_FIELD);
                    return new HeatmapGraphData.HeatMapRow(shortId, shortId, participantGuid, firstName, lastName);
                })
                .collect(Collectors.toList());
    }

    Map<String, String> getParticipantsGuidsWithShortIds(Map<String, Map<String, Object>> ddpParticipantsFromES) {
        Map<String, String> participantsGuidsWithShortIds = new LinkedHashMap<>();
        ddpParticipantsFromES.forEach((k, v) -> {
            String shortId = (String)((Map) v.get(ElasticSearchUtil.PROFILE)).get(ElasticSearchUtil.HRUID);
            participantsGuidsWithShortIds.put(k, shortId);
        });
        return participantsGuidsWithShortIds;
    }

    private Map<String, List<DDPKitRequestDto>> getParticipantsWithKitsCreatedBySystem(Map<String, String> ddpParticipantsFromES) {
        Map<String, List<DDPKitRequestDto>> participantsWithKits = new LinkedHashMap<>();
        DDPKitRequestDao ddpKitRequestDao = new DDPKitRequestDao();
        ddpParticipantsFromES.forEach((pId, shortId) -> participantsWithKits.put(shortId,
                ddpKitRequestDao.getKitRequestsByParticipantId(pId).stream()
                        .filter(ddpKitRequestDto -> "SYSTEM".equals(ddpKitRequestDto.getCreatedBy()))
                        .collect(Collectors.toList()))
        );
        return participantsWithKits;
    }

    Map<String, Map<String, List<Object>>> getParticipantsWithKitsSeparatedByMonth(Map<String, Map<String, Object>> ddpParticipantsFromES,
                                                                                           Map<String, List<DDPKitRequestDto>> participantsWithKits) {
        Map<String, Map<String, List<Object>>> participantsWithKitsSeparatedByMonth = new LinkedHashMap<>();
        participantsWithKits.forEach((pId, kits) -> {
            Map<String, List<Object>> defaultMap = Map.of(
                    "1ST", new ArrayList<>(),
                    "2ND", new ArrayList<>(),
                    "3RD", new ArrayList<>(),
                    "4TH", new ArrayList<>(),
                    "5TH", new ArrayList<>(),
                    "6TH", new ArrayList<>());
            participantsWithKitsSeparatedByMonth.computeIfAbsent(pId, k -> defaultMap);
            kits.forEach(kit -> {
                Map<String, Object> participantEsData = ddpParticipantsFromES.get(kit.getDdpParticipantId());
                Optional<Long> maybeConsentCompletedAt = getConsentCompletedAtOfParticipant(participantEsData);
                maybeConsentCompletedAt.ifPresent(consentCompletedInMillis -> {
                    if ("SHIPPED".equals(kit.getExternalOrderStatus()) || "SHIPPED (SIMULATED)".equals(kit.getExternalOrderStatus())) {
                        sortKitsByMonth(participantsWithKitsSeparatedByMonth, pId, kit, consentCompletedInMillis);
                    }
                });
            });
        });
        return participantsWithKitsSeparatedByMonth;
    }

    Optional<Long> getConsentCompletedAtOfParticipant(Map<String, Object> participantEsData) {
        return ((List) participantEsData.get(ElasticSearchUtil.ACTIVITIES)).stream()
                .filter(i -> "CONSENT".equals(((Map) i).get(ElasticSearchUtil.ACTIVITY_CODE)) && "COMPLETE".equals(((Map) i).get(ElasticSearchUtil.STATUS)))
                .map(i -> ((Map) i).get(ElasticSearchUtil.COMPLETED_AT))
                .findFirst();
    }

    private void sortKitsByMonth(Map<String, Map<String, List<Object>>> participantsWithKitsSeparatedByMonth, String pId, DDPKitRequestDto kit,
                                 Long consentCompletedInMillis) {
        boolean firstKitTime = kit.getExternalOrderDate() > Instant.ofEpochMilli(consentCompletedInMillis).toEpochMilli()
                && kit.getExternalOrderDate() < Instant.ofEpochMilli(consentCompletedInMillis).plus(30, ChronoUnit.DAYS).toEpochMilli();
        boolean secondKitTime = kit.getExternalOrderDate() > Instant.ofEpochMilli(consentCompletedInMillis).plus(30, ChronoUnit.DAYS).toEpochMilli()
                && kit.getExternalOrderDate() < Instant.ofEpochMilli(consentCompletedInMillis).plus(60, ChronoUnit.DAYS).toEpochMilli();
        boolean thirdKitTime = kit.getExternalOrderDate() > Instant.ofEpochMilli(consentCompletedInMillis).plus(60, ChronoUnit.DAYS).toEpochMilli()
                && kit.getExternalOrderDate() < Instant.ofEpochMilli(consentCompletedInMillis).plus(90, ChronoUnit.DAYS).toEpochMilli();
        boolean fourthKitTime = kit.getExternalOrderDate() > Instant.ofEpochMilli(consentCompletedInMillis).plus(90, ChronoUnit.DAYS).toEpochMilli()
                && kit.getExternalOrderDate() < Instant.ofEpochMilli(consentCompletedInMillis).plus(120, ChronoUnit.DAYS).toEpochMilli();
        boolean fifthKitTime = kit.getExternalOrderDate() > Instant.ofEpochMilli(consentCompletedInMillis).plus(120, ChronoUnit.DAYS).toEpochMilli()
                && kit.getExternalOrderDate() < Instant.ofEpochMilli(consentCompletedInMillis).plus(150, ChronoUnit.DAYS).toEpochMilli();
        boolean sixthKitTime = kit.getExternalOrderDate() > Instant.ofEpochMilli(consentCompletedInMillis).plus(150, ChronoUnit.DAYS).toEpochMilli()
                && kit.getExternalOrderDate() < Instant.ofEpochMilli(consentCompletedInMillis).plus(180, ChronoUnit.DAYS).toEpochMilli();
        if (firstKitTime) {
            participantsWithKitsSeparatedByMonth.get(pId).get("1ST").add(kit);
        } else if (secondKitTime) {
            participantsWithKitsSeparatedByMonth.get(pId).get("2ND").add(kit);
        } else if (thirdKitTime) {
            participantsWithKitsSeparatedByMonth.get(pId).get("3RD").add(kit);
        } else if (fourthKitTime) {
            participantsWithKitsSeparatedByMonth.get(pId).get("4TH").add(kit);
        } else if (fifthKitTime) {
            participantsWithKitsSeparatedByMonth.get(pId).get("5TH").add(kit);
        } else if (sixthKitTime) {
            participantsWithKitsSeparatedByMonth.get(pId).get("6TH").add(kit);
        }
    }

    private String getColorRange(List<Object> kits) {
        StringBuilder colorRange = new StringBuilder();
        DDPKitDao ddpKitDao = new DDPKitDao();
        UPShipmentDao upShipmentDao = new UPShipmentDao();
        UPSPackageDao upsPackageDao = new UPSPackageDao();
        UPSActivityDao upsActivityDao = new UPSActivityDao();
        kits.forEach(kit -> {
            Optional<DDPKitDto> maybeDdpKitByDsmKitRequestId =
                    ddpKitDao.getDDPKitByDsmKitRequestId((int) ((DDPKitRequestDto) kit).getDsmKitRequestId());
            maybeDdpKitByDsmKitRequestId.ifPresent(ddpKitDto -> {
                if (StringUtils.isNotBlank(ddpKitDto.getUpsReturnStatus())) {
                    colorRange.setLength(0);
                    colorRange.append(getKitStatus(ddpKitDto.getUpsReturnStatus(), false, ddpKitDto.getUpsReturnDate()));
                } else if (StringUtils.isBlank(ddpKitDto.getUpsReturnStatus())) {
                    Optional<UPShipmentDto> maybeUpshipmentByDsmKitRequestId =
                            upShipmentDao.getUpshipmentByDsmKitRequestId(ddpKitDto.getDsmKitRequestId());
                    maybeUpshipmentByDsmKitRequestId.ifPresent(upShipmentDto -> {
                        List<UPSPackageDto> upsPackageByShipmentId =
                                upsPackageDao.getUpsPackageByShipmentId(upShipmentDto.getUpsShipmentId());
                        Optional<UPSPackageDto> maybeUpsPackageIncoming = upsPackageByShipmentId.stream()
                                .filter(upsPackageDto -> ddpKitDto.getTrackingReturnId().equals(upsPackageDto.getTrackingNumber()))
                                .findFirst();
                        maybeUpsPackageIncoming.ifPresent(upsPackageDto -> {
                            Optional<UPSActivityDto> maybeLatestUpsActivityByPackageId =
                                    upsActivityDao.getLatestUpsActivityByPackageId(upsPackageDto.getUpsPackageId());
                            maybeLatestUpsActivityByPackageId.ifPresent(upsActivityDto -> {
                                colorRange.setLength(0);
                                colorRange.append(getKitStatus(upsActivityDto.getUpsStatusType(), false, upsActivityDto.getUpsActivityDateTime()));
                            });
                        });
                    });
                }
                if (colorRange.length() == 0) {
                    if (StringUtils.isNotBlank(ddpKitDto.getUpsTrackingStatus())
                            && (ddpKitDto.getUpsTrackingStatus().startsWith("M") || ddpKitDto.getUpsTrackingStatus().startsWith("I"))) {
                        colorRange.setLength(0);
                        colorRange.append(getKitStatus(ddpKitDto.getUpsTrackingStatus(), true, ddpKitDto.getUpsTrackingDate()));
                    } else {
                        Optional<UPShipmentDto> maybeUpshipmentByDsmKitRequestId =
                                upShipmentDao.getUpshipmentByDsmKitRequestId(ddpKitDto.getDsmKitRequestId());
                        maybeUpshipmentByDsmKitRequestId.ifPresent(upShipmentDto -> {
                            List<UPSPackageDto> upsPackageByShipmentId =
                                    upsPackageDao.getUpsPackageByShipmentId(upShipmentDto.getUpsShipmentId());
                            Optional<UPSPackageDto> maybeUpsPackageIncoming = upsPackageByShipmentId.stream()
                                    .filter(upsPackageDto -> ddpKitDto.getTrackingToId().equals(upsPackageDto.getTrackingNumber()))
                                    .findFirst();
                            maybeUpsPackageIncoming.ifPresent(upsPackageDto -> {
                                Optional<UPSActivityDto> maybeLatestUpsActivityByPackageId =
                                        upsActivityDao.getLatestUpsActivityByPackageId(upsPackageDto.getUpsPackageId());
                                maybeLatestUpsActivityByPackageId.ifPresent(upsActivityDto -> {
                                    colorRange.setLength(0);
                                    colorRange.append(getKitStatus(upsActivityDto.getUpsStatusType(), true, upsActivityDto.getUpsActivityDateTime()));
                                });
                            });
                        });
                    }
                }
            });
        });
        return colorRange.length() == 0 ? "Not Ordered" : colorRange.toString();
    }

    String getKitStatus(String status, boolean isOutGoing, Object date) {
        String colorRange = "Not Ordered";
        LocalDateTime upsTrackingDate = date instanceof String ? LocalDateTime.parse((String) date, dtf) : (LocalDateTime) date;
        if (isOutGoing) {
            if (status.startsWith("M") || status.startsWith("I")) {
                long elapsedDays = Duration.between(upsTrackingDate, LocalDateTime.now()).abs().toDays();
                if (elapsedDays > 7) colorRange = "Not Delivered";
                else colorRange = "In transit (outgoing)";
            }
        } else {
            if (status.startsWith("D")) {
                colorRange = "Returned";
            } else if (status.startsWith("M") || status.startsWith("I")) {
                long elapsedDays = Duration.between(upsTrackingDate,
                        LocalDateTime.now()).abs().toDays();
                if (elapsedDays > 7) colorRange = "Not Returned";
                else colorRange = "In transit (incoming)";
            }
        }
        return colorRange;
    }

}
