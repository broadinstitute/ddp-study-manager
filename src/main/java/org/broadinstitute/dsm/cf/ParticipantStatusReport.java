package org.broadinstitute.dsm.cf;

import static org.broadinstitute.dsm.model.gbf.GBFOrderGateKeeper.GBF;

import java.io.ByteArrayInputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.cloud.functions.BackgroundFunction;
import com.google.cloud.functions.Context;
import com.google.cloud.storage.Bucket;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import com.typesafe.config.ConfigUtil;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.ddp.db.TransactionWrapper;
import org.broadinstitute.ddp.util.GoogleBucket;
import org.broadinstitute.dsm.DSMServer;
import org.broadinstitute.dsm.statics.ApplicationConfigConstants;
import org.elasticsearch.client.RestHighLevelClient;

public class ParticipantStatusReport implements BackgroundFunction<ParticipantStatusReport.ReportRequest> {

    private static final String PTP_QUERY = "\n" +
            "-- current status and number of unreturned kits\n" +
            "select distinct\n" +
            "u.hruid,\n" +
            "u.guid,\n" +
            "from_unixtime(e.valid_from/1000) as effective_on,\n" +
            "et.enrollment_status_type_code as status,\n" +
            "-- number of returned kits\n" +
            "(select count(distinct k.dsm_kit_id) from\n" +
            "prod_dsm_db.ddp_kit k,\n" +
            "prod_dsm_db.ddp_kit_request req,\n" +
            "prod_dsm_db.kit_type kt\n" +
            "where\n" +
            "req.ddp_participant_id = u.guid\n" +
            "and\n" +
            "req.dsm_kit_request_id = k.dsm_kit_request_id\n" +
            "and\n" +
            "kt.kit_type_id = req.kit_type_id\n" +
            "and\n" +
            "kt.kit_type_name = 'AN'\n" +
            "and\n" +
            "req.order_transmitted_at is not null\n" +
            "and\n" +
            "-- evidence of kit returned\n" +
            "(k.CE_order is not null or k.test_result is not null or exists(\n" +
            "    select 1\n" +
            "    from\n" +
            "    ups_package pack,\n" +
            "    ups_activity act\n" +
            "    where\n" +
            "    pack.tracking_number = k.tracking_return_id\n" +
            "    and\n" +
            "    pack.ups_package_id = act.ups_package_id\n" +
            "    and\n" +
            "    act.ups_activity_date_time = (select max(act2.ups_activity_date_time)\n" +
            "        from\n" +
            "        ups_activity act2\n" +
            "        where\n" +
            "        act2.ups_package_id = act.ups_package_id\n" +
            "        and\n" +
            "        (act2.ups_status_type in ('I', 'D') or act2.ups_status_description = 'Delivered')\n" +
            "    )\n" +
            "))) as all_returned_kits,\n" +
            "(select count(distinct k.dsm_kit_id) from\n" +
            "prod_dsm_db.ddp_kit k,\n" +
            "prod_dsm_db.ddp_kit_request req,\n" +
            "prod_dsm_db.kit_type kt\n" +
            "where\n" +
            "req.ddp_participant_id = u.guid\n" +
            "and\n" +
            "req.dsm_kit_request_id = k.dsm_kit_request_id\n" +
            "and\n" +
            "kt.kit_type_id = req.kit_type_id\n" +
            "and\n" +
            "kt.kit_type_name = 'AN'\n" +
            "and\n" +
            "req.order_transmitted_at is not null\n" +
            "and\n" +
            "-- kit was delivered to participant\n" +
            " exists(\n" +
            "    select 1\n" +
            "    from\n" +
            "    ups_package pack,\n" +
            "    ups_activity act\n" +
            "    where\n" +
            "    pack.tracking_number = k.tracking_to_id\n" +
            "    and\n" +
            "    pack.ups_package_id = act.ups_package_id\n" +
            "    and\n" +
            "    act.ups_activity_date_time = (select max(act2.ups_activity_date_time)\n" +
            "        from\n" +
            "        ups_activity act2\n" +
            "        where\n" +
            "        act2.ups_package_id = act.ups_package_id\n" +
            "        and\n" +
            "        (act2.ups_status_type = 'D' or act2.ups_status_description = 'Delivered')\n" +
            "    ))\n" +
            "and\n" +
            "-- no sign of return\n" +
            "(k.CE_order is null and k.test_result is null and not exists(\n" +
            "    select 1\n" +
            "    from\n" +
            "    ups_package pack,\n" +
            "    ups_activity act\n" +
            "    where\n" +
            "    pack.tracking_number = k.tracking_return_id\n" +
            "    and\n" +
            "    pack.ups_package_id = act.ups_package_id\n" +
            "    and\n" +
            "    act.ups_activity_date_time = (select max(act2.ups_activity_date_time)\n" +
            "        from\n" +
            "        ups_activity act2\n" +
            "        where\n" +
            "        act2.ups_package_id = act.ups_package_id\n" +
            "        and\n" +
            "        (act2.ups_status_type in ('I', 'D') or act2.ups_status_description = 'Delivered')\n" +
            "    )\n" +
            "))) as all_unreturned_kits,\n" +
            "(select count(distinct k.dsm_kit_id) from\n" +
            "prod_dsm_db.ddp_kit k,\n" +
            "prod_dsm_db.ddp_kit_request req,\n" +
            "prod_dsm_db.kit_type kt\n" +
            "where\n" +
            "req.ddp_participant_id = u.guid\n" +
            "and\n" +
            "req.dsm_kit_request_id = k.dsm_kit_request_id\n" +
            "and\n" +
            "kt.kit_type_id = req.kit_type_id\n" +
            "and\n" +
            "kt.kit_type_name = 'AN'\n" +
            "and\n" +
            "req.upload_reason is null) as all_scheduled_kits,\n" +
            "(select count(distinct k.dsm_kit_id) from\n" +
            "prod_dsm_db.ddp_kit k,\n" +
            "prod_dsm_db.ddp_kit_request req,\n" +
            "prod_dsm_db.kit_type kt\n" +
            "where\n" +
            "req.ddp_participant_id = u.guid\n" +
            "and\n" +
            "req.dsm_kit_request_id = k.dsm_kit_request_id\n" +
            "and\n" +
            "kt.kit_type_id = req.kit_type_id\n" +
            "and\n" +
            "kt.kit_type_name = 'AN'\n" +
            "and\n" +
            "req.order_transmitted_at is not null\n" +
            "and\n" +
            "req.upload_reason is null) as all_scheduled_ordered_kits,\n" +
            "(select count(distinct k.dsm_kit_id) from\n" +
            "prod_dsm_db.ddp_kit k,\n" +
            "prod_dsm_db.ddp_kit_request req,\n" +
            "prod_dsm_db.kit_type kt\n" +
            "where\n" +
            "req.ddp_participant_id = u.guid\n" +
            "and\n" +
            "req.dsm_kit_request_id = k.dsm_kit_request_id\n" +
            "and\n" +
            "kt.kit_type_id = req.kit_type_id\n" +
            "and\n" +
            "kt.kit_type_name = 'AN'\n" +
            "and\n" +
            "req.order_transmitted_at is not null\n" +
            "and\n" +
            "req.upload_reason is null\n" +
            "and\n" +
            "req.external_order_status like '%CANCEL%') as all_scheduled_cancelled,\n" +
            "(select count(distinct k.dsm_kit_id) from\n" +
            "prod_dsm_db.ddp_kit k,\n" +
            "prod_dsm_db.ddp_kit_request req,\n" +
            "prod_dsm_db.kit_type kt\n" +
            "where\n" +
            "req.ddp_participant_id = u.guid\n" +
            "and\n" +
            "req.dsm_kit_request_id = k.dsm_kit_request_id\n" +
            "and\n" +
            "kt.kit_type_id = req.kit_type_id\n" +
            "and\n" +
            "kt.kit_type_name = 'AN'\n" +
            "and\n" +
            "req.order_transmitted_at is not null\n" +
            "and\n" +
            "req.upload_reason is null\n" +
            "and\n" +
            "-- kit was delivered to participant\n" +
            " exists(\n" +
            "    select 1\n" +
            "    from\n" +
            "    ups_package pack,\n" +
            "    ups_activity act\n" +
            "    where\n" +
            "    pack.tracking_number = k.tracking_to_id\n" +
            "    and\n" +
            "    pack.ups_package_id = act.ups_package_id\n" +
            "    and\n" +
            "    (act.ups_status_type = 'D' or act.ups_status_description = 'Delivered')\n" +
            "    )) as all_scheduled_delivered,\n" +
            "(select count(distinct k.dsm_kit_id) from\n" +
            "prod_dsm_db.ddp_kit k,\n" +
            "prod_dsm_db.ddp_kit_request req,\n" +
            "prod_dsm_db.kit_type kt\n" +
            "where\n" +
            "req.ddp_participant_id = u.guid\n" +
            "and\n" +
            "req.dsm_kit_request_id = k.dsm_kit_request_id\n" +
            "and\n" +
            "kt.kit_type_id = req.kit_type_id\n" +
            "and\n" +
            "kt.kit_type_name = 'AN'\n" +
            "and\n" +
            "req.order_transmitted_at is not null\n" +
            "and\n" +
            "req.upload_reason is null\n" +
            "and\n" +
            "-- kit was delivered to participant\n" +
            " exists(\n" +
            "    select 1\n" +
            "    from\n" +
            "    ups_package pack,\n" +
            "    ups_activity act\n" +
            "    where\n" +
            "    pack.tracking_number = k.tracking_to_id\n" +
            "    and\n" +
            "    pack.ups_package_id = act.ups_package_id\n" +
            "    and\n" +
            "    act.ups_activity_date_time = (select max(act2.ups_activity_date_time)\n" +
            "        from\n" +
            "        ups_activity act2\n" +
            "        where\n" +
            "        act2.ups_package_id = act.ups_package_id\n" +
            "        and\n" +
            "        (act2.ups_status_type = 'D' or act2.ups_status_description = 'Delivered')\n" +
            "    ))\n" +
            "and\n" +
            "-- no sign of return\n" +
            "(k.CE_order is null and k.test_result is null and not exists(\n" +
            "    select 1\n" +
            "    from\n" +
            "    ups_package pack,\n" +
            "    ups_activity act\n" +
            "    where\n" +
            "    pack.tracking_number = k.tracking_return_id\n" +
            "    and\n" +
            "    pack.ups_package_id = act.ups_package_id\n" +
            "    and\n" +
            "    act.ups_activity_date_time = (select max(act2.ups_activity_date_time)\n" +
            "        from\n" +
            "        ups_activity act2\n" +
            "        where\n" +
            "        act2.ups_package_id = act.ups_package_id\n" +
            "        and\n" +
            "        (act2.ups_status_type in ('I', 'D') or act2.ups_status_description = 'Delivered')\n" +
            "    )\n" +
            "))) as all_scheduled_unreturned,\n" +
            "(select count(distinct k.dsm_kit_id) from\n" +
            "prod_dsm_db.ddp_kit k,\n" +
            "prod_dsm_db.ddp_kit_request req,\n" +
            "prod_dsm_db.kit_type kt\n" +
            "where\n" +
            "req.ddp_participant_id = u.guid\n" +
            "and\n" +
            "req.dsm_kit_request_id = k.dsm_kit_request_id\n" +
            "and\n" +
            "kt.kit_type_id = req.kit_type_id\n" +
            "and\n" +
            "kt.kit_type_name = 'AN'\n" +
            "and\n" +
            "req.order_transmitted_at is not null\n" +
            "and\n" +
            "req.upload_reason is not null) as all_manual_kits,\n" +
            "(select count(distinct k.dsm_kit_id) from\n" +
            "prod_dsm_db.ddp_kit k,\n" +
            "prod_dsm_db.ddp_kit_request req,\n" +
            "prod_dsm_db.kit_type kt\n" +
            "where\n" +
            "req.ddp_participant_id = u.guid\n" +
            "and\n" +
            "req.dsm_kit_request_id = k.dsm_kit_request_id\n" +
            "and\n" +
            "kt.kit_type_id = req.kit_type_id\n" +
            "and\n" +
            "kt.kit_type_name = 'AN'\n" +
            "and\n" +
            "req.order_transmitted_at is not null\n" +
            "and\n" +
            "req.upload_reason is not null\n" +
            "and\n" +
            "-- evidence of kit returned\n" +
            "(k.CE_order is not null or k.test_result is not null or exists(\n" +
            "    select 1\n" +
            "    from\n" +
            "    ups_package pack,\n" +
            "    ups_activity act\n" +
            "    where\n" +
            "    pack.tracking_number = k.tracking_return_id\n" +
            "    and\n" +
            "    pack.ups_package_id = act.ups_package_id\n" +
            "    and\n" +
            "    act.ups_activity_date_time = (select max(act2.ups_activity_date_time)\n" +
            "        from\n" +
            "        ups_activity act2\n" +
            "        where\n" +
            "        act2.ups_package_id = act.ups_package_id\n" +
            "        and\n" +
            "        (act2.ups_status_type in ('I', 'D') or act2.ups_status_description = 'Delivered')\n" +
            "    )\n" +
            "))) as all_manual_returned_kits\n" +
            "from\n" +
            "pepperapisprod.enrollment_status_type et,\n" +
            "pepperapisprod.user_study_enrollment e,\n" +
            "pepperapisprod.user u,\n" +
            "pepperapisprod.umbrella_study s\n" +
            "where\n" +
            "s.umbrella_study_id = e.study_id\n" +
            "and\n" +
            "s.guid = ?\n" +
            "and\n" +
            "u.user_id = e.user_id\n" +
            "and\n" +
            "e.enrollment_status_type_id = et.enrollment_status_type_id\n" +
            "and\n" +
            "e.valid_to is null";


    @Override
    public void accept(ParticipantStatusReport.ReportRequest reportRequest, Context context) throws Exception {
        // run the query and write out to bucket location
        String ddpInstance = System.getenv("DDP_INSTANCE");
        String googleProject = System.getenv("PROJECT_ID");
        String bucket = System.getenv("REPORT_BUCKET");
        String filePath = System.getenv("REPORT_PATH");
        Config cfg = CFUtil.loadConfig();
        DSMServer.setupExternalShipperLookup(cfg.getString(ApplicationConfigConstants.EXTERNAL_SHIPPER));
        String dbUrl = cfg.getString(ApplicationConfigConstants.DSM_DB_URL);

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

        TransactionWrapper.init(5, dbUrl, cfg, true);

        String fileName = "participant-status-" + dateFormat.format(System.currentTimeMillis()) + ".csv";
        StringBuilder reportBuilder = new StringBuilder();
        final AtomicInteger numRows = new AtomicInteger(0);
        TransactionWrapper.inTransaction(conn -> {
            try (PreparedStatement stmt = conn.prepareStatement(PTP_QUERY)) {
                stmt.setString(1,ddpInstance);

                String headerRow = StringUtils.join(new String[] {"hruid","guid","status","effective date","all returned", "all unreturned", "all scheduled",
                        "all scheduled ordered", "all scheduled cancelled","all scheduled delivered","all scheduled unreturned", "all manual kits","all manual returned"},",");

                reportBuilder.append(headerRow).append("\n");

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        String hruid = rs.getString("hruid");
                        String participantGuid = rs.getString("guid");
                        String status = rs.getString("status");
                        Timestamp effectiveDate = rs.getTimestamp("effective_on");
                        String allReturnedKits = rs.getString("all_returned_kits");
                        String allUnreturnedKits = rs.getString("all_unreturned_kits");
                        String allScheduledKits = rs.getString("all_scheduled_kits");
                        String allScheduledOrderedKits = rs.getString("all_scheduled_ordered_kits");
                        String allScheduledCancelled = rs.getString("all_scheduled_cancelled");
                        String allScheduledDelivered = rs.getString("all_scheduled_delivered");
                        String allScheduledUnreturned = rs.getString("all_scheduled_unreturned");
                        String allManualReturned = rs.getString("all_manual_returned_kits");
                        String allManualKits = rs.getString("all_manual_kits");

                        String rowData = StringUtils.join(new String[] {hruid,participantGuid, status, effectiveDate.toString(), allReturnedKits, allUnreturnedKits,
                                allScheduledKits, allScheduledOrderedKits,allScheduledCancelled,allScheduledDelivered,
                                allScheduledUnreturned,allManualKits, allManualReturned}, ",");
                        reportBuilder.append(rowData).append("\n");
                        numRows.incrementAndGet();
                    }
                }
            } catch(SQLException e) {
                throw new RuntimeException("Could not generate report", e);
            }
            return null;
        });

        String response = GoogleBucket.uploadFile(null, googleProject, bucket, filePath + "/" + fileName, new ByteArrayInputStream(reportBuilder.toString().getBytes()));

        System.out.println(reportBuilder.toString());
        System.out.println("Wrote " + numRows.get() + " rows to " + response);
    }

    public class ReportRequest {

    }
}
