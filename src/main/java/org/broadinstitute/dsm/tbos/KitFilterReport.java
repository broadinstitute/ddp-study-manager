package org.broadinstitute.dsm.tbos;

import java.io.ByteArrayInputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.ddp.db.TransactionWrapper;
import org.broadinstitute.ddp.util.GoogleBucket;

public class KitFilterReport {
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

    public static void main(String[] args) {
        // run the query and write out to bucket location
        String googleProject = args[1];
        String ddpInstance = args[0];
        String bucket = args[2];
        String filePath = args[3];
        String dbUrl = args[4];
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Map<String, String> fileNameToQuery = new HashMap<>();
        Config cfg = ConfigFactory.load();
        fileNameToQuery.put("recently-sent-orders-" + dateFormat.format(System.currentTimeMillis()) + ".csv",RECENTLY_SENT);
        fileNameToQuery.put("recently-filtered-orders-" + dateFormat.format(System.currentTimeMillis()) + ".csv", RECENTLY_FILTERED);


        TransactionWrapper.init(5, dbUrl, cfg, true);

        for (Map.Entry<String, String> fileNameQuery : fileNameToQuery.entrySet()) {
            String fileName = fileNameQuery.getKey();
            String query = fileNameQuery.getValue();

            StringBuilder reportBuilder = new StringBuilder();
            final AtomicInteger numRows = new AtomicInteger(0);
            TransactionWrapper.inTransaction(conn -> {
                try (PreparedStatement stmt = conn.prepareStatement(query)) {
                    stmt.setString(1,ddpInstance);

                    String headerRow = StringUtils.join(new String[] {"hruid","guid","status","effective date","all returned", "all unreturned", "all scheduled",
                            "all scheduled ordered", "all scheduled cancelled","all scheduled delivered","all scheduled unreturned", "all manual kits","all manual returned"},",");

                    reportBuilder.append(headerRow).append("\n");

                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            String hruid = rs.getString("hruid");
                            String externalOrderNumber = rs.getString("external_order_number");

                            String rowData = StringUtils.join(new String[] {hruid,externalOrderNumber}, ",");
                            reportBuilder.append(rowData).append("\n");
                            numRows.incrementAndGet();
                        }
                    }
                } catch(SQLException e) {
                    throw new RuntimeException("Could not generate report", e);
                }

                String response = GoogleBucket.uploadFile(null, googleProject, bucket, filePath + "/" + fileName, new ByteArrayInputStream(reportBuilder.toString().getBytes()));

                System.out.println(reportBuilder.toString());
                System.out.println("Wrote " + numRows.get() + " rows to " + response);

                return null;
            });
        }



    }
}
