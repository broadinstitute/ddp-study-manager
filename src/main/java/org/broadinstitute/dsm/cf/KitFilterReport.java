package org.broadinstitute.dsm.cf;

import java.io.ByteArrayInputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.cloud.functions.BackgroundFunction;
import com.google.cloud.functions.Context;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.apache.commons.dbcp2.PoolableConnection;
import org.apache.commons.dbcp2.PoolingDataSource;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.ddp.db.TransactionWrapper;
import org.broadinstitute.ddp.util.GoogleBucket;
import org.broadinstitute.dsm.DSMServer;
import org.broadinstitute.dsm.statics.ApplicationConfigConstants;

public class KitFilterReport implements BackgroundFunction<KitFilterReport.ReportRequest> {

    private static final String RECENTLY_SENT =
            "select distinct u.hruid, req.external_order_number\n"+
                    "from\n"+
                    "prod_dsm_db.ddp_kit_request req,\n"+
                    "pepperapisprod.user u,\n"+
                    "prod_dsm_db.ddp_instance i\n"+
                    "where\n"+
                    "i.instance_name = ?\n"+
                    "and\n"+
                    "i.ddp_instance_id = req.ddp_instance_id\n"+
                    "and\n"+
                    "u.guid = req.ddp_participant_id\n"+
                    "and\n"+
                    "req.order_transmitted_at >= DATE_ADD(now(), interval -7 day )\n"+
                    "and\n"+
                    "req.upload_reason is null";

    private static final String RECENTLY_FILTERED =
            "select distinct u.hruid, req.external_order_number\n"+
                    "from\n"+
                    "pepperapisprod.user u,\n"+
                    "prod_dsm_db.ddp_kit_request req,\n"+
                    "prod_dsm_db.ddp_instance i\n"+
                    "where\n"+
                    "i.instance_name = ?\n"+
                    "and\n"+
                    "i.ddp_instance_id = req.ddp_instance_id\n"+
                    "and\n"+
                    "u.guid = req.ddp_participant_id\n"+
                    "and\n"+
                    "from_unixtime(req.created_date/1000) >=  DATE_ADD(now(), interval -7 day )\n"+
                    "and\n"+
                    "upload_reason is null\n"+
                    "and\n"+
                    "req.order_transmitted_at is null";


    @Override
    public void accept(KitFilterReport.ReportRequest reportRequest, Context context) throws Exception {
        // run the query and write out to bucket location
        String ddpInstance = System.getenv("DDP_INSTANCE");
        String googleProject = System.getenv("PROJECT_ID");
        String bucket = System.getenv("REPORT_BUCKET");
        String filePath = System.getenv("REPORT_PATH");

        Config cfg = CFUtil.loadConfig();

        DSMServer.setupExternalShipperLookup(cfg.getString(ApplicationConfigConstants.EXTERNAL_SHIPPER));
        String dbUrl = cfg.getString(ApplicationConfigConstants.DSM_DB_URL);

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Map<String, String> fileNameToQuery = new HashMap<>();
        fileNameToQuery.put("recently-sent-orders-" + dateFormat.format(System.currentTimeMillis()) + ".csv",RECENTLY_SENT);
        fileNameToQuery.put("recently-filtered-orders-" + dateFormat.format(System.currentTimeMillis()) + ".csv", RECENTLY_FILTERED);

        PoolingDataSource<PoolableConnection> dataSource = CFUtil.createDataSource(2, dbUrl);

        for (Map.Entry<String, String> fileNameQuery : fileNameToQuery.entrySet()) {
            String fileName = fileNameQuery.getKey();
            String query = fileNameQuery.getValue();

            StringBuilder reportBuilder = new StringBuilder();
            final AtomicInteger numRows = new AtomicInteger(0);
            try (Connection conn = dataSource.getConnection()) {
                try (PreparedStatement stmt = conn.prepareStatement(query)) {
                    stmt.setString(1, ddpInstance);

                    String headerRow = StringUtils.join(new String[] {"hruid", "guid", "status", "effective date", "all returned", "all unreturned", "all scheduled",
                            "all scheduled ordered", "all scheduled cancelled", "all scheduled delivered", "all scheduled unreturned", "all manual kits", "all manual returned"}, ",");

                    reportBuilder.append(headerRow).append("\n");

                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            String hruid = rs.getString("hruid");
                            String externalOrderNumber = rs.getString("external_order_number");

                            String rowData = StringUtils.join(new String[] {hruid, externalOrderNumber}, ",");
                            reportBuilder.append(rowData).append("\n");
                            numRows.incrementAndGet();
                        }
                    }
                } catch (SQLException e) {
                    throw new RuntimeException("Could not generate report", e);
                }

                String response = GoogleBucket.uploadFile(null, googleProject, bucket, filePath + "/" + fileName, new ByteArrayInputStream(reportBuilder.toString().getBytes()));

                System.out.println(reportBuilder.toString());
                System.out.println("Wrote " + numRows.get() + " rows to " + response);
            }
        }
    }

    public static class ReportRequest {

    }
}
