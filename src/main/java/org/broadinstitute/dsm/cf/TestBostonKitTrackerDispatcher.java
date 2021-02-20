package org.broadinstitute.dsm.cf;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.cloud.functions.BackgroundFunction;
import com.google.cloud.functions.Context;
import com.google.cloud.secretmanager.v1.SecretManagerServiceClient;
import com.google.cloud.secretmanager.v1.SecretPayload;
import com.google.cloud.secretmanager.v1.SecretVersionName;
import com.google.pubsub.v1.PubsubMessage;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.apache.commons.dbcp2.ConnectionFactory;
import org.apache.commons.dbcp2.DriverManagerConnectionFactory;
import org.apache.commons.dbcp2.PoolableConnection;
import org.apache.commons.dbcp2.PoolableConnectionFactory;
import org.apache.commons.dbcp2.PoolingDataSource;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.broadinstitute.dsm.model.ups.UPSStatus;

/**
 * Identifies kits whose shipping history should be
 * queried (by another job) in order to produce
 * reports on turnaround time and study adherence.
 */
public class TestBostonKitTrackerDispatcher implements BackgroundFunction<PubsubMessage> {

    private static final Logger logger = Logger.getLogger(TestBostonKitTrackerDispatcher.class.getName());

    private String STUDY_MANAGER_SCHEMA = System.getenv("STUDY_MANAGER_SCHEMA") + ".";
    private String STUDY_SERVER_SCHEMA = System.getenv("STUDY_SERVER_SCHEMA") + ".";
    private static final String SQL_SELECT_KITS = "SELECT kit.dsm_kit_request_id, kit.kit_label, kit.tracking_to_id, kit.tracking_return_id, kit.error, kit.message, kit.receive_date, kit.ups_tracking_status, kit.ups_tracking_date, kit.ups_return_date, kit.ups_return_status, req.bsp_collaborator_participant_id, " +
            " req.external_order_number, kit.CE_order FROM ddp_kit kit LEFT JOIN ddp_kit_request req " +
            " ON (kit.dsm_kit_request_id = req.dsm_kit_request_id) WHERE req.ddp_instance_id = ? and kit_label not like \"%\\\\_1\"  ";

    private static String SQL_AVOID_DELIVERED = " and (tracking_to_id is not null or tracking_return_id is not null ) and (ups_tracking_status is null or ups_return_status is null or ups_tracking_status not like \"" + UPSStatus.DELIVERED_TYPE + " %\" or ups_return_status not like \"" + UPSStatus.DELIVERED_TYPE + " %\")" +
            " order by kit.dsm_kit_request_id ASC";


    @Override
    public void accept(PubsubMessage pubsubMessage, Context context) throws Exception {

//        String KIT_QUERY =
//                "select " +
//                "u.guid, " +
//                "k.kit_label, " +
//                "from_unixtime(dkr.created_date/1000) as kit_requested_at, " +
//                "k.tracking_to_id, " +
//                "k.tracking_return_id, " +
//                "json_extract(k.test_result, '$[0].result') test_result, " +
//                "ifnull(STR_TO_DATE(replace(json_extract(k.test_result, '$[0].timeCompleted'),'\"',''), '%Y-%m-%dT%H:%i:%sZ'), " +
//                "    STR_TO_DATE(replace(json_extract(k.test_result, '$[0].timeCompleted'),'\"',''), '%Y-%m-%dT%H:%i:%s.%fZ') " +
//                ")as test_completion_time, " +
//                "u.hruid, " +
//                "dkr.upload_reason " +
//                "from " +
//                STUDY_MANAGER_SCHEMA + "ddp_kit k, " +
//                STUDY_MANAGER_SCHEMA + "ddp_kit_request dkr, " +
//                STUDY_SERVER_SCHEMA + "user u, " +
//                STUDY_MANAGER_SCHEMA + "ddp_instance study, " +
//                STUDY_MANAGER_SCHEMA + "kit_type kt " +
//                "where " +
//                "study.instance_name = 'testboston' " +
//                "and " +
//                "u.guid = dkr.ddp_participant_id " +
//                "and " +
//                "study.ddp_instance_id = dkr.ddp_instance_id " +
//                "and " +
//                "dkr.dsm_kit_request_id = k.dsm_kit_request_id " +
//                "and " +
//                "kt.kit_type_id = dkr.kit_type_id " +
//                "and " +
//                "kt.kit_type_name = 'AN' " +
//                "and k.kit_label like 'TBOS-%' " +
//                "order by test_completion_time";




        Config cfg = CFUtil.loadConfig();
        String dbUrl = cfg.getString("pepperDbUrl");

        PoolingDataSource<PoolableConnection> dataSource = CFUtil.createDataSource(1, dbUrl);

        // static vars are dangerous in CF https://cloud.google.com/functions/docs/bestpractices/tips#functions-tips-scopes-java
        try (Connection conn = dataSource.getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(SQL_SELECT_KITS + SQL_AVOID_DELIVERED)) {
                stmt.setFetchSize(1000);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        String kitLabel = rs.getString("kit.kit_label");
                        logger.info(kitLabel);

                        // spawn threads or other cloud functions in fixed size blocks? Write to a separate queue for worker threads
                        // to grab history?
                    }
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Trouble querying data", e);
        }
    }




}
