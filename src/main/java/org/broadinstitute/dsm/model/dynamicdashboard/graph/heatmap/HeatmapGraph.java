package org.broadinstitute.dsm.model.dynamicdashboard.graph.heatmap;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
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
import org.broadinstitute.dsm.model.dynamicdashboard.StatisticResult;
import org.broadinstitute.dsm.model.dynamicdashboard.graph.Graph;
import org.broadinstitute.dsm.model.dynamicdashboard.graph.GraphResult;
import org.broadinstitute.dsm.util.ElasticSearchUtil;

public class HeatmapGraph extends Graph {
    @Override
    public StatisticResult filter(StatisticPayload statisticPayload) {
        HeatmapGraphData heatmapResult = new HeatmapGraphData();
        switch (statisticPayload.getFilterType()) {
            case KIT_STATUSES:
                heatmapResult = this.samplesByStatuses(statisticPayload);
                break;

        }
        return heatmapResult;
    }

    private HeatmapGraphData samplesByStatuses(StatisticPayload statisticPayload) {
        DDPInstance ddpInstanceByRealm = DDPInstanceDao.getDDPInstanceByRealm(statisticPayload.getRealm());

        Map<String, Map<String, Object>> ddpParticipantsFromES =
                ElasticSearchUtil.getDDPParticipantsFromESWithRange(ddpInstanceByRealm.getName(), ddpInstanceByRealm.getParticipantIndexES(),
                        statisticPayload.getFrom(), statisticPayload.getTo());
        Map<String, String> participantsGuidsWithShortIds = new HashMap<>();
        ddpParticipantsFromES.forEach((k,v) -> {
            String shortId = (String)((Map) v.get("profile")).get("hruid");
            participantsGuidsWithShortIds.put(k, shortId);
        });
        Map<String, List<DDPKitRequestDto>> participantsWithKits = getParticipantsWithKitsCreatedBySystem(participantsGuidsWithShortIds);
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
        List<HeatmapGraphData.HeatMapRow> heatmapGraphRows = participantsWithKitsSeparatedByMonth.keySet().stream()
                .map(shortId -> new HeatmapGraphData.HeatMapRow(shortId, shortId, Participant
                        .getParticipantGuidByHruid(ddpParticipantsFromES, shortId)))
                .collect(Collectors.toList());

        List<HeatmapGraphData.HeatMapDataSet> heatMapDataSet = new ArrayList<>();
        participantsWithKitsSeparatedByMonth.forEach(
                (pId, value) -> value.forEach((k, v) -> heatMapDataSet.add(new HeatmapGraphData.HeatMapDataSet(pId, k, getColorRange(v)))));

        List<Map<String, Object>> columns = getColumns(statisticPayload);
        Map<String, Object> colorRange = getColorRange(statisticPayload);
        return new HeatmapGraphData(heatmapGraphRows, columns, colorRange, heatMapDataSet, statisticPayload.getDisplayType(), "Kit Statuses");
    }

    private List<Map<String, Object>> getColumns(StatisticPayload statisticPayload) {
        return (List<Map<String, Object>>) statisticPayload.getPossibleValues().stream()
                .filter(pv -> pv.containsKey("columns"))
                .findAny()
                .orElse(Map.of())
                .get("columns");
    }

    private Map<String, Object> getColorRange(StatisticPayload statisticPayload) {
        return (Map<String, Object>) statisticPayload.getPossibleValues().stream()
                .filter(pv -> pv.containsKey("colorrange"))
                .findAny()
                .orElse(Map.of())
                .get("colorrange");
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

    private Optional<Long> getConsentCompletedAtOfParticipant(Map<String, Object> participantEsData) {
        return ((List) participantEsData.get("activities")).stream()
                .filter(i -> "CONSENT".equals(((Map) i).get("activityCode")) && "COMPLETE".equals(((Map) i).get("status")))
                .map(i -> ((Map) i).get("completedAt"))
                .findFirst();
    }

    private Map<String, List<DDPKitRequestDto>> getParticipantsWithKitsCreatedBySystem(Map<String, String> ddpParticipantsFromES) {
        Map<String, List<DDPKitRequestDto>> participantsWithKits = new HashMap<>();
        ddpParticipantsFromES.forEach((pId, shortId) -> participantsWithKits.put(shortId,
                new DDPKitRequestDao().getKitRequestsByParticipantId(pId).stream()
                        .filter(ddpKitRequestDto -> "SYSTEM".equals(ddpKitRequestDto.getCreatedBy()))
                        .collect(Collectors.toList()))
        );
        return  participantsWithKits;
    }

    private String getColorRange(List<Object> kits) {
        String result = "Not Ordered";
        if (kits.size() == 0) return result;
        AtomicBoolean isReturned = new AtomicBoolean(false);
        AtomicBoolean isInTransitIncoming = new AtomicBoolean(false);
        AtomicBoolean isNotReturned = new AtomicBoolean(false);
        AtomicBoolean isIntransitOutgoing = new AtomicBoolean(false);
        AtomicBoolean isNotDelivered = new AtomicBoolean(false);
        kits.forEach(kit -> {
            Optional<DDPKitDto> maybeDdpKitByDsmKitRequestId =
                    new DDPKitDao().getDDPKitByDsmKitRequestId((int) ((DDPKitRequestDto) kit).getDsmKitRequestId());
            maybeDdpKitByDsmKitRequestId.ifPresent(ddpKitDto -> {
                DateTimeFormatter dtf = new DateTimeFormatterBuilder().appendPattern("yyyyMMdd HHmmss").toFormatter();
                if (StringUtils.isNotBlank(ddpKitDto.getUpsReturnStatus())) {
                    LocalDateTime upsReturnDate = LocalDateTime.parse(ddpKitDto.getUpsReturnDate(), dtf);
                    if (ddpKitDto.getUpsReturnStatus().startsWith("D")) {
                        isReturned.set(true);
                    } else if (ddpKitDto.getUpsReturnStatus().startsWith("M") || ddpKitDto.getUpsReturnStatus().startsWith("I")) {
                        long elapsedDays = Duration.between(LocalDateTime.now(), upsReturnDate.plus(1, ChronoUnit.DAYS)).abs().toDays();
                        if (elapsedDays > 7) isNotReturned.set(true);
                        else isInTransitIncoming.set(true);
                    }
                } else if (StringUtils.isBlank(ddpKitDto.getUpsReturnStatus())) {
                    Optional<UPShipmentDto> upshipmentByDsmKitRequestId =
                            new UPShipmentDao().getUpshipmentByDsmKitRequestId(ddpKitDto.getDsmKitRequestId());
                    upshipmentByDsmKitRequestId.ifPresent(upShipmentDto -> {
                        List<UPSPackageDto> upsPackageByShipmentId =
                                new UPSPackageDao().getUpsPackageByShipmentId(upShipmentDto.getUpsShipmentId());
                        Optional<UPSPackageDto> maybeUpsPackageIncoming = upsPackageByShipmentId.stream()
                                .filter(upsPackageDto -> ddpKitDto.getTrackingReturnId().equals(upsPackageDto.getTrackingNumber()))
                                .findFirst();
                        maybeUpsPackageIncoming.ifPresent(upsPackageDto -> {
                            Optional<UPSActivityDto> latestUpsActivityByPackageId =
                                    new UPSActivityDao().getLatestUpsActivityByPackageId(upsPackageDto.getUpsPackageId());
                            latestUpsActivityByPackageId.ifPresent(upsActivityDto -> {
                                if ("D".equals(upsActivityDto.getUpsStatusType())) {
                                    isReturned.set(true);
                                } else if ("I".equals(upsActivityDto.getUpsStatusType()) || "M".equals(upsActivityDto.getUpsStatusType())) {
                                    long elapsedDays = Duration.between(LocalDateTime.now(),
                                            upsActivityDto.getUpsActivityDateTime().plus(1, ChronoUnit.DAYS)).abs().toDays();
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
                        long elapsedDays = Duration.between(LocalDateTime.now(), upsTrackingDate).abs().toDays();
                        if (elapsedDays > 7) isNotDelivered.set(true);
                        else isIntransitOutgoing.set(true);
                    } else {
                        Optional<UPShipmentDto> upshipmentByDsmKitRequestId =
                                new UPShipmentDao().getUpshipmentByDsmKitRequestId(ddpKitDto.getDsmKitRequestId());
                        upshipmentByDsmKitRequestId.ifPresent(upShipmentDto -> {
                            List<UPSPackageDto> upsPackageByShipmentId =
                                    new UPSPackageDao().getUpsPackageByShipmentId(upShipmentDto.getUpsShipmentId());
                            Optional<UPSPackageDto> maybeUpsPackageIncoming = upsPackageByShipmentId.stream()
                                    .filter(upsPackageDto -> ddpKitDto.getTrackingToId().equals(upsPackageDto.getTrackingNumber()))
                                    .findFirst();
                            maybeUpsPackageIncoming.ifPresent(upsPackageDto -> {
                                Optional<UPSActivityDto> latestUpsActivityByPackageId =
                                        new UPSActivityDao().getLatestUpsActivityByPackageId(upsPackageDto.getUpsPackageId());
                                latestUpsActivityByPackageId.ifPresent(upsActivityDto -> {
                                    if ("I".equals(upsActivityDto.getUpsStatusType()) || "M".equals(upsActivityDto.getUpsStatusType())) {
                                        long elapsedDays = Duration.between(LocalDateTime.now(),
                                                upsActivityDto.getUpsActivityDateTime().plus(1, ChronoUnit.DAYS)).abs().toDays();
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
        if (isReturned.get()) result = "Returned";
        else if (isInTransitIncoming.get()) result = "In transit (incoming)";
        else if (isNotReturned.get()) result = "Not Returned";
        else if (isNotDelivered.get()) result = "Not Delivered";
        else if (isIntransitOutgoing.get()) result = "In transit (outgoing)";
        return result;
    }
}