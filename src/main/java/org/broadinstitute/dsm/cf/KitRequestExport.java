package org.broadinstitute.dsm.cf;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.broadinstitute.dsm.model.KitRequest;

public class KitRequestExport {

    private static final String ALL_KIT_REQUESTS =
        "select\n" +
        "u.guid,\n" +
        "u.hruid,\n" +
        "req.external_order_status,\n" +
        "(select from_unixtime(pkr.created_at) from pepperapisprod.kit_request pkr where pkr.kit_request_guid = req.ddp_kit_request_id) as kit_scheduled_date,\n" +
        "req.order_transmitted_at,\n" +
        "-- add result directly, not from shipping\n" +
        "(select k.kit_label from ddp_kit k where k.dsm_kit_request_id = req.dsm_kit_request_id) as kit_label, -- get kit label directly)\n" +
        "json_extract((select k.test_result from ddp_kit k where k.dsm_kit_request_id = req.dsm_kit_request_id), '$[0].result') test_result,\n" +
        "ifnull(STR_TO_DATE(replace(json_extract((select k.test_result from ddp_kit k where k.dsm_kit_request_id = req.dsm_kit_request_id), '$[0].timeCompleted'),'\\\\',''), '%Y-%m-%dT%H:%i:%sZ'),\n" +
        "            STR_TO_DATE(replace(json_extract((select k.test_result from ddp_kit k where k.dsm_kit_request_id = req.dsm_kit_request_id), '$[0].timeCompleted'),'\\\\',''), '%Y-%m-%dT%H:%i:%s.%fZ')\n" +
        ")as test_completion_time,\n" +
        "(select pkr.kit_request_guid from pepperapisprod.kit_request pkr where pkr.kit_request_guid = req.ddp_kit_request_id) as kit_request_guid,\n" +
        "req.upload_reason,\n" +
        "from_unixtime(req.created_date/1000) as kit_requested_at,\n" +
        "req.external_order_number,\n" +
        "req.upload_reason\n" +
        "from\n" +
        "pepperapisprod.user u\n" +
        "left join ddp_kit_request req on req.ddp_participant_id = u.guid\n" +
        "left join ddp_instance i on i.ddp_instance_id = req.ddp_instance_id\n" +
        "left join kit_type kt on kt.kit_type_id = req.kit_type_id\n" +
        "where\n" +
        "i.instance_name = ?\n" +
        "and kt.kit_type_name = ?";

    private static final String ALL_KITS =
        "select\n" +
        "req.external_order_number,\n" +
        "k.dsm_kit_request_id,\n" +
        "act.ups_status_type,\n" +
        "act.ups_status_description,\n" +
        "act.ups_status_code,\n" +
        "act.ups_activity_date_time,\n" +
        "(if(k.tracking_return_id = pack.tracking_number, 'return','outbound')) as direction,\n" +
        "u.guid,\n" +
        "u.hruid\n" +
        "from\n" +
        "ddp_kit k,\n" +
        "pepperapisprod.user u,\n" +
        "ups_package pack,\n" +
        "ups_activity act,\n" +
        "ddp_kit_request req,\n" +
        "ddp_instance i,\n" +
        "kit_type kt\n" +
        "where\n" +
        "(k.tracking_return_id = pack.tracking_number or k.tracking_to_id = pack.tracking_number)\n" +
        "and\n" +
        "kt.kit_type_id = req.kit_type_id\n" +
        "and\n" +
        "kt.kit_type_name = ?\n" +
        "and\n" +
        "u.guid = req.ddp_participant_id\n" +
        "and\n" +
        "i.instance_name = ?\n" +
        "and\n" +
        "i.ddp_instance_id = req.ddp_instance_id\n" +
        "and\n" +
        "req.dsm_kit_request_id = k.dsm_kit_request_id\n" +
        "and\n" +
        "act.ups_package_id = pack.ups_package_id";

    private Map<String, KitRequestDetails> getAllKitRequests(Connection conn, String ddpInstance, String kitType) {
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

    private void doIt(Connection conn, String ddpInstance, String kitType) {
        // get all kit requests and all kit details
        Map<String, KitRequestDetails> kitRequestByExternalKitNumber = getAllKitRequests(conn, ddpInstance, kitType);
        Map<String, KitDetails> kitByExternalKitNumber = getAllKits(conn, ddpInstance, kitType);
        Map<String, Participant> participantMap = new HashMap<>();


        try (PreparedStatement stmt = conn.prepareStatement(ALL_KITS)) {
            stmt.setString(1, ddpInstance);
            stmt.setString(2, kitType);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    KitDetails kitDetails = toKit(rs);
                    if (!kitByExternalKitNumber.containsKey(kitDetails.getExternalOrderNumber())) {
                        kitByExternalKitNumber.put(kitDetails.getExternalOrderNumber(), kitDetails);
                    }
                    kitByExternalKitNumber.get(kitDetails.getExternalOrderNumber()).addPackageEvent();

                }
            }
        }

        // iterate through kit requests to declare participants, and then
        // add kit details (if known) for the kit requests for each participant
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

        for (Participant participant : participantMap.values()) {
            String participantRow = participant.asRow();
            System.out.println(participantRow);
        }
    }

    private KitDetails toKit(ResultSet rs) {
        return new KitDetails()
    }

    private Map<String, KitDetails> getAllKits(Connection conn, String ddpInstance, String kitType) {

    }

    private KitRequestDetails toKitRequest(ResultSet rs) {
        return new KitRequestDetails(rs.getString("external_order_number"),
                rs.getString("external_order_status"),
                rs.getTimestamp("kit_scheduled_date").toInstant(),
                rs.getTimestamp("order_transmitted_at").toInstant(),
                rs.getString("kit_request_guid"),
                rs.getString("upload_reason"));

    }


    public void writeReport(Collection<Participant> participants,
                            String scheduledFile,
                            String adhocFile) {
        for (Participant participant : participants) {
            if (participant.isScheduled()) {

            } else {

            }
        }

    }

    private enum PackageDirection {
        INBOUND,OUTBOUND
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

        public String asRow() {
            StringBuilder rowBuilder = new StringBuilder();
            rowBuilder.append(guid).append(",").append(shortId).append(",");

            for (KitRequestDetails kitRequest : getKitRequests()) {
                rowBuilder.append(kitRequest.getExternalKitNumber()).append(",");
                if (kitRequest.getKitDetails() != null) {
                    KitDetails kitDetails = kitRequest.getKitDetails();
                    rowBuilder.append(kitDetails.getKitLabel()).append(",");
                }
            }
            rowBuilder.append("\n");
            return rowBuilder.toString();
        }

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

        private final String kitLabel;
        private final String testResult;
        private final Instant resultDate;
        private final String externalOrderNumber;
        private Instant deliveredAt;

        public KitDetails(String kitLabel,
                          String testResult,
                          Instant resultDate,
                          String externalOrderNumber) {
            this.kitLabel = kitLabel;
            this.testResult = testResult;
            this.resultDate = resultDate;
            this.externalOrderNumber = externalOrderNumber;
        }

        public void addPackageEvent(String statusType,
                                    String statusCode,
                                    String statusDescription,
                                    Instant eventTime,
                                    PackageDirection direction) {
            // I, O, D, out for delivery

            // check for latest delivery, is on its way
            if (direction == PackageDirection.OUTBOUND) {
                if (DELIVERED.equals(statusDescription) || OUT_FOR_DELIVERY.equals(statusDescription) ||
                        D.equals(statusType) || I.equals(statusType) || O.equals(statusType)) {
                    if (deliveredAt == null) {
                        deliveredAt = eventTime;
                    } else {
                        if (eventTime.isBefore(deliveredAt)) {
                            deliveredAt = eventTime;
                        }
                    }
                }
                if ()
            }
        }

        public String getKitLabel() {
            return kitLabel;
        }

        public String getTestResult() {
            return testResult;
        }

        public Instant getResultDate() {
            return resultDate;
        }

        public String getExternalOrderNumber() {
            return externalOrderNumber;
        }

        public Instant getShippedAt() {

        }

        public Instant getDeliveredAt() {

        }

        public Instant getPickedUpAt() {

        }

        public Instant getReceivedAt() {

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
                                 String participantGuid) {

            this.externalKitNumber = externalKitNumber;
            this.externalStatus = externalStatus;
            this.scheduledDate = scheduledDate;
            this.orderTransmittedDate = orderTransmittedDate;
            this.kitRequestGuid = kitRequestGuid;
            this.uploadReason = uploadReason;
            this.hruid = hruid;
            this.participantGuid = participantGuid;
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

        public String getUploadReason() {
            return uploadReason;
        }

        public void setKitDetails(KitDetails kitDetails) {
            this.kitDetails = kitDetails;
        }

        public KitDetails getKitDetails() {
            return kitDetails;
        }
    }
}



