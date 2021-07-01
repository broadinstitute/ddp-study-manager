package org.broadinstitute.dsm.cf;

import static org.broadinstitute.dsm.model.ups.UPSStatus.DELIVERED_TYPE;
import static org.broadinstitute.dsm.model.ups.UPSStatus.IN_TRANSIT_TYPE;
import static org.broadinstitute.dsm.model.ups.UPSStatus.OUT_FOR_DELIVERY_TYPE;
import static org.broadinstitute.dsm.model.ups.UPSStatus.PICKUP_TYPE;

import java.io.ByteArrayInputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.google.cloud.functions.BackgroundFunction;
import com.google.cloud.functions.Context;
import com.typesafe.config.Config;
import org.apache.commons.dbcp2.PoolableConnection;
import org.apache.commons.dbcp2.PoolingDataSource;
import org.broadinstitute.ddp.util.GoogleBucket;
import org.broadinstitute.dsm.statics.ApplicationConfigConstants;

public class KitRequestExport implements BackgroundFunction<KitRequestExport.ReportRequest> {

    private static final String ALL_KIT_REQUESTS =
            "select\n" +
                    "u.guid,\n" +
                    "u.hruid,\n" +
                    "req.external_order_status,\n" +
                    "(select from_unixtime(pkr.created_at) from pepperapisprod.kit_request pkr where pkr.kit_request_guid = req.ddp_kit_request_id) as kit_scheduled_date,\n" +
                    "req.order_transmitted_at,\n" +
                    "-- add result directly, not from shipping\n" +
                    "(select k.kit_label from ddp_kit k where k.dsm_kit_request_id = req.dsm_kit_request_id) as kit_label, -- get kit label directly)\n" +
                    "(select pkr.kit_request_guid from pepperapisprod.kit_request pkr where pkr.kit_request_guid = req.ddp_kit_request_id) as kit_request_guid,\n" +
                    "req.upload_reason,\n" +
                    "from_unixtime(req.created_date/1000) as kit_requested_at,\n" +
                    "req.external_order_number,\n" +
                    "req.external_order_status,\n" +
                    "req.upload_reason\n" +
                    "from\n" +
                    "pepperapisprod.user u\n" +
                    "left join ddp_kit_request req on req.ddp_participant_id = u.guid\n" +
                    "left join ddp_instance i on i.ddp_instance_id = req.ddp_instance_id\n" +
                    "left join kit_type kt on kt.kit_type_id = req.kit_type_id\n" +
                    "where\n" +
                    "i.instance_name = ?\n" +
                    "and kt.kit_type_name = ?";

    private static final String ALL_KITS = "select req.external_order_number,\n" +
            "       k.dsm_kit_request_id,\n" +
            "       act.ups_status_type,\n" +
            "       act.ups_status_description,\n" +
            "       act.ups_status_code,\n" +
            "       CONVERT_TZ(act.ups_activity_date_time, 'US/Eastern', 'UTC')           as ups_activity_date_time,\n" +
            "       json_extract((select k.test_result from ddp_kit k where k.dsm_kit_request_id = req.dsm_kit_request_id), '$[0].result') test_result,\n" +
            "ifnull(STR_TO_DATE(replace(json_extract((select k.test_result from ddp_kit k where k.dsm_kit_request_id = req.dsm_kit_request_id), '$[0].timeCompleted'),'\\\\',''), '\"%Y-%m-%dT%H:%i:%sZ\"'),\n" +
            "            STR_TO_DATE(replace(json_extract((select k.test_result from ddp_kit k where k.dsm_kit_request_id = req.dsm_kit_request_id), '$[0].timeCompleted'),'\\\\',''), '\"%Y-%m-%dT%H:%i:%s.%fZ\"')\n" +
            ")as test_completion_time,\n" +
            "       (if(k.tracking_return_id = pack.tracking_number, 'RETURN', 'OUTBOUND')) as direction,\n" +
            "       u.guid,\n" +
            "       k.kit_label,\n" +
            "        req.external_order_status,\n" +
            "        u.hruid\n" +
            "from\n" +
            "    ddp_kit k,\n" +
            "    pepperapisprod.user u,\n" +
            "    ups_package pack,\n" +
            "    ups_activity act,\n" +
            "    ddp_kit_request req,\n" +
            "    ddp_instance i,\n" +
            "    kit_type kt\n" +
            "where\n" +
            "    (k.tracking_return_id = pack.tracking_number\n" +
            "   or k.tracking_to_id = pack.tracking_number)\n" +
            "  and\n" +
            "    kt.kit_type_id = req.kit_type_id\n" +
            "  and\n" +
            "    kt.kit_type_name = ?\n" +
            "  and\n" +
            "    u.guid = req.ddp_participant_id\n" +
            "  and\n" +
            "    i.instance_name = ?\n" +
            "  and\n" +
            "    i.ddp_instance_id = req.ddp_instance_id\n" +
            "  and\n" +
            "    req.dsm_kit_request_id = k.dsm_kit_request_id\n" +
            "  and\n" +
            "    act.ups_package_id = pack.ups_package_id\n" +
            "\n" +
            "\n" +
            "union\n" +
            "\n" +
            "select req.external_order_number,\n" +
            "       k.dsm_kit_request_id,\n" +
            "       null,\n" +
            "       null,\n" +
            "       null,\n" +
            "       null,\n" +
            "       json_extract((select k.test_result from ddp_kit k where k.dsm_kit_request_id = req.dsm_kit_request_id), '$[0].result') test_result,\n" +
            "ifnull(STR_TO_DATE(replace(json_extract((select k.test_result from ddp_kit k where k.dsm_kit_request_id = req.dsm_kit_request_id), '$[0].timeCompleted'),'\\\\',''), '\"%Y-%m-%dT%H:%i:%sZ\"'),\n" +
            "            STR_TO_DATE(replace(json_extract((select k.test_result from ddp_kit k where k.dsm_kit_request_id = req.dsm_kit_request_id), '$[0].timeCompleted'),'\\\\',''), '\"%Y-%m-%dT%H:%i:%s.%fZ\"')\n" +
            ")as test_completion_time,\n" +
            "       null as direction,\n" +
            "       u.guid,\n" +
            "       k.kit_label,\n" +
            "        req.external_order_status,\n" +
            "        u.hruid\n" +
            "from\n" +
            "    ddp_kit k,\n" +
            "    pepperapisprod.user u,\n" +
            "    ups_package pack,\n" +
            "    ddp_kit_request req,\n" +
            "    ddp_instance i,\n" +
            "    kit_type kt\n" +
            "where\n" +
            "    (k.tracking_return_id = pack.tracking_number\n" +
            "   or k.tracking_to_id = pack.tracking_number)\n" +
            "  and\n" +
            "    kt.kit_type_id = req.kit_type_id\n" +
            "  and\n" +
            "    kt.kit_type_name = ?\n" +
            "  and\n" +
            "    u.guid = req.ddp_participant_id\n" +
            "  and\n" +
            "    i.instance_name = ?\n" +
            "  and\n" +
            "    i.ddp_instance_id = req.ddp_instance_id\n" +
            "  and\n" +
            "    req.dsm_kit_request_id = k.dsm_kit_request_id\n" +
            "  and\n" +
            "    not exists (select 1 from ups_activity act where act.ups_package_id = pack.ups_package_id)\n";


    private Map<String, KitRequestDetails> getAllKitRequests(Connection conn, String ddpInstance, String kitType) throws SQLException {
        Map<String, KitRequestDetails> kitRequestByExternalKitNumber = new HashMap<>();
        try (PreparedStatement stmt = conn.prepareStatement(ALL_KIT_REQUESTS)) {
            stmt.setString(1, ddpInstance);
            stmt.setString(2, kitType);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    KitRequestDetails kitRequest = toKitRequest(rs);
                    kitRequestByExternalKitNumber.put(kitRequest.getExternalKitNumber(), kitRequest);
                }
            }
        }
        return kitRequestByExternalKitNumber;
    }

    // do same for scheduled kits

    private void doIt(Connection conn, String ddpInstance, String kitType, String googleProject, String bucket, String filePath) throws SQLException {
        // get all kit requests and all kit details
        System.out.println("Getting kit requests");
        Map<String, KitRequestDetails> kitRequestByExternalKitNumber = getAllKitRequests(conn, ddpInstance, kitType);
        System.out.println("Getting kits");
        Map<String, KitDetails> kitByExternalKitNumber = getAllKits(conn, ddpInstance, kitType);
        Map<String, Participant> participantMap = new HashMap<>();


        // iterate through kit requests to declare participants, and then
        // add kit details (if known) for the kit requests for each participant
        System.out.println("Iterating");
        for (Map.Entry<String, KitRequestDetails> kitRequestEntry : kitRequestByExternalKitNumber.entrySet()) {
            KitRequestDetails kitRequest = kitRequestEntry.getValue();
            if (!participantMap.containsKey(kitRequest.getParticipantGuid())) {
                participantMap.put(kitRequest.getParticipantGuid(), new Participant(kitRequest.getHruid(), kitRequest.getParticipantGuid()));
            }
            Participant participant = participantMap.get(kitRequest.getParticipantGuid());

            // if there's a kit for the request, set it
            if (kitByExternalKitNumber.containsKey(kitRequest.getExternalKitNumber())) {
                kitRequest.setKitDetails(kitByExternalKitNumber.get(kitRequest.getExternalKitNumber()));
            }
            // add the kit request to the participant
            participant.addKitRequest(kitRequest);
        }

        writeReport(participantMap.values(), googleProject, bucket, filePath);
    }

    private KitDetails toKit(ResultSet rs) throws SQLException {
        Timestamp testCompletionTimestamp = rs.getTimestamp("test_completion_time");
        Instant testCompletionTime = null;

        testCompletionTime = testCompletionTimestamp != null ? testCompletionTimestamp.toInstant() : null;
        return new KitDetails(rs.getString("kit_label"),
                rs.getString("test_result"),
                testCompletionTime,
                rs.getString("external_order_number"),
                rs.getString("external_order_status"));
    }

    private Map<String, KitDetails> getAllKits(Connection conn, String ddpInstance, String kitType) throws SQLException {
        Map<String, KitDetails> kitByExternalKitNumber = new HashMap<>();
        try (PreparedStatement stmt = conn.prepareStatement(ALL_KITS)) {
            stmt.setString(1, kitType);
            stmt.setString(3, kitType);
            stmt.setString(2, ddpInstance);
            stmt.setString(4, ddpInstance);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    KitDetails kitDetails = toKit(rs);
                    if (!kitByExternalKitNumber.containsKey(kitDetails.getExternalOrderNumber())) {
                        kitByExternalKitNumber.put(kitDetails.getExternalOrderNumber(), kitDetails);
                    }

                    // sometimes there is no kit history, so don't add events if they don't exist
                    if (rs.getString("direction") != null) {
                        Timestamp upsTimestamp = rs.getTimestamp("ups_activity_date_time");
                        kitByExternalKitNumber.get(kitDetails.getExternalOrderNumber()).addPackageEvent(
                                rs.getString("ups_status_type"),
                                rs.getString("ups_status_code"),
                                rs.getString("ups_status_description"),
                                upsTimestamp != null ? upsTimestamp.toInstant() : null,
                                PackageDirection.valueOf(rs.getString("direction")));
                    }
                }
            }
        }
        return kitByExternalKitNumber;
    }

    private KitRequestDetails toKitRequest(ResultSet rs) throws SQLException {

        Timestamp scheduledTimestamp = rs.getTimestamp("kit_scheduled_date");
        Instant scheduledTime = scheduledTimestamp != null ? scheduledTimestamp.toInstant() : null;
        Timestamp transmittedTimestamp = rs.getTimestamp("order_transmitted_at");
        Instant transmittedTime = transmittedTimestamp != null ? transmittedTimestamp.toInstant() : null;
        return new KitRequestDetails(rs.getString("external_order_number"),
                rs.getString("external_order_status"),
                scheduledTime,
                transmittedTime,
                rs.getString("kit_request_guid"),
                rs.getString("upload_reason"),
                rs.getString("hruid"),
                rs.getString("guid"),
                rs.getTimestamp("kit_requested_at").toInstant());

    }

    public void writeReport(Collection<Participant> participants, String googleProject, String bucket, String filePath) {
        StringBuilder adhocBuilder = new StringBuilder();
        StringBuilder scheduledBuilder = new StringBuilder();

        int maxKitRequests = 0;
        for (Participant participant : participants) {
            int numScheduledKits = participant.getScheduledKitRequests().size();
            int numAdhocKits = participant.getAdHocKitRequests().size();

            if (numScheduledKits > maxKitRequests) {
                maxKitRequests = numScheduledKits;
            }
            if (numAdhocKits > maxKitRequests) {
                maxKitRequests = numAdhocKits;
            }
        }

        writeHeaderRow(adhocBuilder, maxKitRequests);
        writeHeaderRow(scheduledBuilder, maxKitRequests);

        for (Participant participant : participants) {
            List<KitRequestDetails> kitRequests = participant.getAdHocKitRequests();
            if (!kitRequests.isEmpty()) {
                appendParticipantData(participant, adhocBuilder, kitRequests);
            }
            kitRequests = participant.getScheduledKitRequests();
            if (!kitRequests.isEmpty()) {
                appendParticipantData(participant, scheduledBuilder, kitRequests);
            }
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");


        String adhocFile = "adhoc-kits-" + dateFormat.format(System.currentTimeMillis()) + ".csv";
        String scheduledFile = "scheduled-kits-" + dateFormat.format(System.currentTimeMillis()) + ".csv";

        String response = GoogleBucket.uploadFile(null, googleProject, bucket, filePath + "/" + adhocFile, new ByteArrayInputStream(adhocBuilder.toString().getBytes()));

        System.out.println("Wrote " + response);
        response = GoogleBucket.uploadFile(null, googleProject, bucket, filePath + "/" + scheduledFile, new ByteArrayInputStream(scheduledBuilder.toString().getBytes()));

        System.out.println("Wrote " + response);
    }

    private void writeHeaderRow(StringBuilder reportBuilder, int maxKitRequests) {
        reportBuilder.append("participant guid,hruid,");
        for (int i = 0; i < maxKitRequests; i++) {
            reportBuilder.append("external order number,ddp kit request id,kit label,scheduled date,shipper response,reason,result,requested at,shipped at,delivered at,picked up at,received at,resulted at,");
        }
        reportBuilder.append("\n");
    }

    private void appendParticipantData(Participant participant, StringBuilder reportBuilder, List<KitRequestDetails> kitRequests) {
        reportBuilder.append(participant.getGuid()).append(",").append(participant.getShortId()).append(",");
        for (KitRequestDetails kitRequestDetails : kitRequests) {
            KitDetails kitDetails = kitRequestDetails.getKitDetails();
            reportBuilder.append(kitRequestDetails.getExternalKitNumber()).append(",");
            reportBuilder.append(dashIfNull(kitRequestDetails.getKitRequestGuid())).append(",");
            if (kitDetails != null) {
                reportBuilder.append(kitDetails.getKitLabel());
            } else {
                reportBuilder.append("-");
            }
            reportBuilder.append(",");
            reportBuilder.append(dashIfNull(kitRequestDetails.getScheduledDate())).append(",");
            reportBuilder.append(dashIfNull(kitRequestDetails.getExternalStatus())).append(",");

            reportBuilder.append(dashIfNull(kitRequestDetails.getUploadReason())).append(",");
            if (kitDetails != null) {
                reportBuilder.append(dashIfNull(kitDetails.getTestResult())).append(",");
            } else {
                reportBuilder.append("-").append(",");
            }
            reportBuilder.append(dashIfNull(kitRequestDetails.getCreatedDate())).append(",");
            if (kitDetails != null) {
                reportBuilder.append(dashIfNull(kitDetails.getShippedAt())).append(",");
            } else {
                reportBuilder.append("-").append(",");
            }
            if (kitDetails != null) {
                reportBuilder.append(dashIfNull(kitDetails.getDeliveredAt())).append(",");
            } else {
                reportBuilder.append("-").append(",");
            }
            if (kitDetails != null) {
                reportBuilder.append(dashIfNull(kitDetails.getPickedUpAt())).append(",");
            } else {
                reportBuilder.append("-").append(",");
            }
            if (kitDetails != null) {
                reportBuilder.append(dashIfNull(kitDetails.getReceivedAt())).append(",");
            } else {
                reportBuilder.append("-").append(",");
            }
            if (kitDetails != null) {
                reportBuilder.append(dashIfNull(kitDetails.getResultDate())).append(",");
            } else {
                reportBuilder.append("-").append(",");
            }
        }
        reportBuilder.append("\n");
    }

    private Object dashIfNull(Object obj) {
        return obj != null ? obj : "-";
    }

    @Override
    public void accept(ReportRequest reportRequest, Context context) throws Exception {
        Config cfg = CFUtil.loadConfig();
        String dbUrl = cfg.getString(ApplicationConfigConstants.DSM_DB_URL);

        String ddpInstance = System.getenv("DDP_INSTANCE");
        String googleProject = System.getenv("PROJECT_ID");
        String bucket = System.getenv("REPORT_BUCKET");
        String filePath = System.getenv("REPORT_PATH");
        String kitType = System.getenv("KIT_TYPE");

        PoolingDataSource<PoolableConnection> dataSource = CFUtil.createDataSource(2, dbUrl);
        try (Connection conn = dataSource.getConnection()) {
            doIt(conn, ddpInstance, kitType, googleProject, bucket, filePath);
        }
    }

    private enum PackageDirection {
        RETURN,OUTBOUND
    }

    private static class Participant {

        private final String shortId;
        private final String guid;
        private Set<KitRequestDetails> kitRequestDetails = new TreeSet<>(new KitCreationDateComparator());

        public Participant(String shortId,
                           String guid) {

            this.shortId = shortId;
            this.guid = guid;
        }

        public void addKitRequest(KitRequestDetails kitRequest) {
            this.kitRequestDetails.add(kitRequest);
        }

        public Collection<KitRequestDetails> getKitRequests() {
            return kitRequestDetails;
        }

        public List<KitRequestDetails> getScheduledKitRequests() {
            return kitRequestDetails.stream().filter(kitRequest ->  kitRequest.isScheduledKit()).collect(Collectors.toList());
        }

        public List<KitRequestDetails> getAdHocKitRequests() {
            return kitRequestDetails.stream().filter(kitRequest ->  !kitRequest.isScheduledKit()).collect(Collectors.toList());
        }

        public String getShortId() {
            return shortId;
        }

        public String getGuid() {
            return guid;
        }

        /**
         * Used for sorting kit requests so that from left to right, kits are in
         * order of creation time
         */
        private class KitCreationDateComparator implements Comparator<KitRequestDetails> {

            @Override
            public int compare(KitRequestDetails req1, KitRequestDetails req2) {
                if (req1.getDateForSorting().isAfter(req2.getDateForSorting())) {
                    return 1;
                } else if (req1.getDateForSorting().isBefore(req2.getDateForSorting())) {
                    return -1;
                } else {
                    return 0;
                }
            }
        }
    }


    private static class KitDetails {

        private static final String DELIVERED = "Delivered";
        private static final String OUT_FOR_DELIVERY = "Out for Delivery";
        private static final String PICKED_UP = "Picked up";
        private final String kitLabel;
        private final String testResult;
        private final Instant resultDate;
        private final String externalOrderNumber;
        private final String shipperReponse;
        private Instant deliveredAt;
        private Instant shippedAt;
        private Instant pickedUpAt;
        private Instant receivedAt;

        public KitDetails(String kitLabel,
                          String testResult,
                          Instant resultDate,
                          String externalOrderNumber,
                          String shipperReponse) {
            this.kitLabel = kitLabel;
            this.testResult = testResult;
            this.resultDate = resultDate;
            this.externalOrderNumber = externalOrderNumber;
            this.shipperReponse = shipperReponse;
        }

        public String getShipperReponse() {
            return shipperReponse != null ? shipperReponse : "-";
        }

        private boolean isDelivery(String statusType, String statusDescription) {
            return DELIVERED.equals(statusDescription) || DELIVERED_TYPE.equals(statusType);
        }

        private boolean isInTransit(String statusType, String statusDescription) {
            return isDelivery(statusType, statusDescription) ||
                    IN_TRANSIT_TYPE.equals(statusType) || OUT_FOR_DELIVERY_TYPE.equals(statusType)
                    || PICKUP_TYPE.equals(statusType) || PICKED_UP.equals(statusDescription) || OUT_FOR_DELIVERY.equals(statusDescription);
        }

        // todo arz check utc time

        public void addPackageEvent(String statusType,
                                    String statusCode,
                                    String statusDescription,
                                    Instant eventTime,
                                    PackageDirection direction) {
            // I, O, D, out for delivery

            // check for latest delivery, is on its way
            boolean isInTransit = isInTransit(statusType, statusDescription);
            boolean isDelivered = isDelivery(statusType, statusDescription);
            if (eventTime != null) {
                if (direction == PackageDirection.OUTBOUND) {
                    if (isInTransit) {
                        if (shippedAt == null) {
                            shippedAt = eventTime;
                        } else {
                            if (eventTime.isBefore(shippedAt)) {
                                shippedAt = eventTime;
                            }
                        }
                    }
                    if (isDelivered) {
                        if (deliveredAt == null) {
                            deliveredAt = eventTime;
                        } else {
                            if (eventTime.isBefore(deliveredAt)) {
                                deliveredAt = eventTime;
                            }
                        }
                    }
                } else if (direction == PackageDirection.RETURN) {
                    if (isInTransit) {
                        if (pickedUpAt == null) {
                            pickedUpAt = eventTime;
                        } else {
                            if (eventTime.isBefore(pickedUpAt)) {
                                pickedUpAt = eventTime;
                            }
                        }
                    }
                    if (isDelivered) {
                        if (receivedAt == null) {
                            receivedAt = eventTime;
                        } else {
                            if (eventTime.isBefore(receivedAt)) {
                                receivedAt = eventTime;
                            }
                        }
                    }
                }
            }
        }

        public String getKitLabel() {
            return kitLabel;
        }

        public String getTestResult() {
            return (testResult != null) ? testResult : "-";
        }

        public Instant getResultDate() {
            return resultDate;
        }

        public String getExternalOrderNumber() {
            return externalOrderNumber;
        }

        public Instant getShippedAt() {
            return shippedAt;
        }

        public Instant getDeliveredAt() {
            return deliveredAt;
        }

        public Instant getPickedUpAt() {
            return pickedUpAt;
        }

        public Instant getReceivedAt() {
            return receivedAt;
        }
    }

    private static class PackageEvent {

    }

    private static class KitRequestDetails {

        private final String externalKitNumber;
        private final String externalStatus;
        private final Instant createdDate;
        private final Instant scheduledDate;
        private final Instant orderTransmittedDate;
        private final String kitRequestGuid;
        private final String uploadReason;
        private final String hruid;
        private final String participantGuid;
        private KitDetails kitDetails;



        public KitRequestDetails(String externalKitNumber,
                                 String externalStatus,
                                 Instant scheduledDate,
                                 Instant orderTransmittedDate,
                                 String kitRequestGuid,
                                 String uploadReason,
                                 String hruid,
                                 String participantGuid,
                                 Instant createdDate) {

            this.externalKitNumber = externalKitNumber;
            this.externalStatus = externalStatus;
            this.scheduledDate = scheduledDate;
            this.orderTransmittedDate = orderTransmittedDate;
            this.kitRequestGuid = kitRequestGuid;
            this.uploadReason = uploadReason;
            this.hruid = hruid;
            this.participantGuid = participantGuid;
            this.createdDate = createdDate;
        }

        public String getHruid() {
            return hruid;
        }

        public String getParticipantGuid() {
            return participantGuid;
        }

        public String getExternalKitNumber() {
            return externalKitNumber;
        }

        public String getExternalStatus() {
            return externalStatus;
        }

        public Instant getDateForSorting() {
            return scheduledDate != null ? scheduledDate : createdDate;
        }

        public Instant getScheduledDate() {
            return scheduledDate;
        }

        public Instant getOrderTransmittedDate() {
            return orderTransmittedDate;
        }

        public String getKitRequestGuid() {
            return kitRequestGuid;
        }

        public Instant getCreatedDate() {
            return createdDate;
        }

        public String getUploadReason() {
            return uploadReason;
        }

        public void setKitDetails(KitDetails kitDetails) {
            this.kitDetails = kitDetails;
        }

        public KitDetails getKitDetails() {
            return kitDetails;
        }

        public boolean isScheduledKit() {
            return uploadReason == null;
        }
    }

    public class ReportRequest {

    }
}



