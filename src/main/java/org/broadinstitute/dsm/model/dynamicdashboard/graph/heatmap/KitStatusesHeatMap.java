package org.broadinstitute.dsm.model.dynamicdashboard.graph.heatmap;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
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

    public KitStatusesHeatMap(StatisticPayload statisticPayload) {
        super(statisticPayload);
    }

    public HeatmapGraphData samplesByStatuses() {
        DDPInstance ddpInstanceByRealm = DDPInstanceDao.getDDPInstanceByRealm(getStatisticPayload().getRealm());

        Map<String, Map<String, Object>> ddpParticipantsFromES =
                ElasticSearchUtil.getDDPParticipantsFromESWithRange(ddpInstanceByRealm.getName(), ddpInstanceByRealm.getParticipantIndexES(),
                        getStatisticPayload().getFrom(), getStatisticPayload().getTo());

        Map<String, String> participantsGuidsWithShortIds = getParticipantsGuidsWithShortIds(ddpParticipantsFromES);

        Map<String, List<DDPKitRequestDto>> participantsWithKits = getParticipantsWithKitsCreatedBySystem(participantsGuidsWithShortIds);

        Map<String, Map<String, List<Object>>> participantsWithKitsSeparatedByMonth =
                getParticipantsWithKitsSeparatedByMonth(ddpParticipantsFromES, participantsWithKits);

        List<HeatmapGraphData.HeatMapRow> heatmapGraphRows = participantsWithKitsSeparatedByMonth.keySet().stream()
                .map(shortId -> new HeatmapGraphData.HeatMapRow(shortId, shortId, Participant
                        .getParticipantGuidByHruidFromParticipantESData(ddpParticipantsFromES, shortId)))
                .collect(Collectors.toList());

        List<HeatmapGraphData.HeatMapDataSet> heatMapDataSet = new ArrayList<>();
        participantsWithKitsSeparatedByMonth.forEach(
                (pId, value) -> value.forEach((k, v) -> heatMapDataSet.add(new HeatmapGraphData.HeatMapDataSet(pId, k, getColorRange(v)))));

        List<Map<String, Object>> columns = getColumns();
        Map<String, Object> colorRange = getColorRange();
        return new HeatmapGraphData(heatmapGraphRows, columns, colorRange, heatMapDataSet, getStatisticPayload().getDisplayType(), getStatisticPayload().getDisplayText());
    }

    private Map<String, String> getParticipantsGuidsWithShortIds(Map<String, Map<String, Object>> ddpParticipantsFromES) {
        Map<String, String> participantsGuidsWithShortIds = new HashMap<>();
        ddpParticipantsFromES.forEach((k, v) -> {
            String shortId = (String)((Map) v.get(ElasticSearchUtil.PROFILE)).get(ElasticSearchUtil.HRUID);
            participantsGuidsWithShortIds.put(k, shortId);
        });
        return participantsGuidsWithShortIds;
    }

    private Map<String, List<DDPKitRequestDto>> getParticipantsWithKitsCreatedBySystem(Map<String, String> ddpParticipantsFromES) {
        Map<String, List<DDPKitRequestDto>> participantsWithKits = new HashMap<>();
        ddpParticipantsFromES.forEach((pId, shortId) -> participantsWithKits.put(shortId,
                new DDPKitRequestDao().getKitRequestsByParticipantId(pId).stream()
                        .filter(ddpKitRequestDto -> "SYSTEM".equals(ddpKitRequestDto.getCreatedBy()))
                        .collect(Collectors.toList()))
        );
        return participantsWithKits;
    }

    private Map<String, Map<String, List<Object>>> getParticipantsWithKitsSeparatedByMonth(Map<String, Map<String, Object>> ddpParticipantsFromES,
                                                                                           Map<String, List<DDPKitRequestDto>> participantsWithKits) {
        Map<String, Map<String, List<Object>>> participantsWithKitsSeparatedByMonth = new HashMap<>();
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

    private Optional<Long> getConsentCompletedAtOfParticipant(Map<String, Object> participantEsData) {
        return ((List) participantEsData.get("activities")).stream()
                .filter(i -> "CONSENT".equals(((Map) i).get("activityCode")) && "COMPLETE".equals(((Map) i).get("status")))
                .map(i -> ((Map) i).get("completedAt"))
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
        String colorRange = "Not Ordered";
        if (kits.size() == 0) return colorRange;
        AtomicBoolean isReturned = new AtomicBoolean(false);
        AtomicBoolean isInTransitIncoming = new AtomicBoolean(false);
        AtomicBoolean isNotReturned = new AtomicBoolean(false);
        AtomicBoolean isIntransitOutgoing = new AtomicBoolean(false);
        AtomicBoolean isNotDelivered = new AtomicBoolean(false);
        DDPKitDao ddpKitDao = new DDPKitDao();
        UPShipmentDao upShipmentDao = new UPShipmentDao();
        UPSPackageDao upsPackageDao = new UPSPackageDao();
        UPSActivityDao upsActivityDao = new UPSActivityDao();
        kits.forEach(kit -> {
            Optional<DDPKitDto> maybeDdpKitByDsmKitRequestId =
                    ddpKitDao.getDDPKitByDsmKitRequestId((int) ((DDPKitRequestDto) kit).getDsmKitRequestId());
            maybeDdpKitByDsmKitRequestId.ifPresent(ddpKitDto -> {
                DateTimeFormatter dtf = new DateTimeFormatterBuilder().appendPattern("yyyyMMdd HHmmss").toFormatter();
                if (StringUtils.isNotBlank(ddpKitDto.getUpsReturnStatus())) {
                    LocalDateTime upsReturnDate = LocalDateTime.parse(ddpKitDto.getUpsReturnDate(), dtf);
                    if (ddpKitDto.getUpsReturnStatus().startsWith("D")) {
                        isReturned.set(true);
                    } else if (ddpKitDto.getUpsReturnStatus().startsWith("M") || ddpKitDto.getUpsReturnStatus().startsWith("I")) {
                        long elapsedDays = Duration.between(upsReturnDate, LocalDateTime.now()).abs().toDays();
                        if (elapsedDays > 7) isNotReturned.set(true);
                        else isInTransitIncoming.set(true);
                    }
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
                                if ("D".equals(upsActivityDto.getUpsStatusType())) {
                                    isReturned.set(true);
                                } else if ("I".equals(upsActivityDto.getUpsStatusType()) || "M".equals(upsActivityDto.getUpsStatusType())) {
                                    long elapsedDays = Duration.between(upsActivityDto.getUpsActivityDateTime(),
                                            LocalDateTime.now()).abs().toDays();
                                    if (elapsedDays > 7) isNotReturned.set(true);
                                    else isInTransitIncoming.set(true);
                                }
                            });
                        });
                    });
                }
                if (!isReturned.get() && !isNotReturned.get() && !isInTransitIncoming.get()) {
                    if (StringUtils.isNotBlank(ddpKitDto.getUpsTrackingStatus())
                            && (ddpKitDto.getUpsTrackingStatus().startsWith("M") || ddpKitDto.getUpsTrackingStatus().startsWith("I"))) {
                        LocalDateTime upsTrackingDate = LocalDateTime.parse(ddpKitDto.getUpsTrackingDate(), dtf);
                        long elapsedDays = Duration.between(upsTrackingDate, LocalDateTime.now()).abs().toDays();
                        if (elapsedDays > 7) isNotDelivered.set(true);
                        else isIntransitOutgoing.set(true);
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
                                    if ("I".equals(upsActivityDto.getUpsStatusType()) || "M".equals(upsActivityDto.getUpsStatusType())) {
                                        long elapsedDays = Duration.between(upsActivityDto.getUpsActivityDateTime(),
                                                LocalDateTime.now()).abs().toDays();
                                        if (elapsedDays > 7) isNotDelivered.set(true);
                                        else isIntransitOutgoing.set(true);
                                    }
                                });
                            });
                        });
                    }
                }
            });
        });
        if (isReturned.get()) colorRange = "Returned";
        else if (isInTransitIncoming.get()) colorRange = "In transit (incoming)";
        else if (isNotReturned.get()) colorRange = "Not Returned";
        else if (isNotDelivered.get()) colorRange = "Not Delivered";
        else if (isIntransitOutgoing.get()) colorRange = "In transit (outgoing)";
        return colorRange;
    }



}
