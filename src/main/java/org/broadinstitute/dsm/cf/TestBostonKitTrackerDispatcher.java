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

/**
 * Identifies kits whose shipping history should be
 * queried (by another job) in order to produce
 * reports on turnaround time and study adherence.
 */
public class TestBostonKitTrackerDispatcher implements BackgroundFunction<PubsubMessage> {

    private static final Logger logger = Logger.getLogger(TestBostonKitTrackerDispatcher.class.getName());

    private String STUDY_MANAGER_SCHEMA = System.getenv("STUDY_MANAGER_SCHEMA") + ".";
    private String STUDY_SERVER_SCHEMA = System.getenv("STUDY_SERVER_SCHEMA") + ".";


    @Override
    public void accept(PubsubMessage pubsubMessage, Context context) throws Exception {
        String projectId = System.getenv("PROJECT_ID");
        String secretId = System.getenv("SECRET_ID");


        String KIT_QUERY =
                "select\n" +
                "u.guid,\n" +
                "k.kit_label,\n" +
                "from_unixtime(dkr.created_date/1000) as kit_requested_at,\n" +
                "k.tracking_to_id,\n" +
                "k.tracking_return_id,\n" +
                "json_extract(k.test_result, '$[0].result') test_result,\n" +
                "ifnull(STR_TO_DATE(replace(json_extract(k.test_result, '$[0].timeCompleted'),'\"',''), '%Y-%m-%dT%H:%i:%sZ'),\n" +
                "    STR_TO_DATE(replace(json_extract(k.test_result, '$[0].timeCompleted'),'\"',''), '%Y-%m-%dT%H:%i:%s.%fZ')\n" +
                ")as test_completion_time,\n" +
                "u.hruid,\n" +
                "dkr.upload_reason\n" +
                "from\n" +
                STUDY_MANAGER_SCHEMA + "ddp_kit k,\n" +
                STUDY_MANAGER_SCHEMA + "ddp_kit_request dkr,\n" +
                STUDY_SERVER_SCHEMA + "user u,\n" +
                STUDY_MANAGER_SCHEMA + "ddp_instance study,\n" +
                STUDY_MANAGER_SCHEMA + "kit_type kt\n" +
                "where\n" +
                "study.instance_name = 'testboston'\n" +
                "and\n" +
                "u.guid = dkr.ddp_participant_id\n" +
                "and\n" +
                "study.ddp_instance_id = dkr.ddp_instance_id\n" +
                "and\n" +
                "dkr.dsm_kit_request_id = k.dsm_kit_request_id\n" +
                "and\n" +
                "kt.kit_type_id = dkr.kit_type_id\n" +
                "and\n" +
                "kt.kit_type_name = 'AN'\n" +
                "and k.kit_label like 'TBOS-%'\n" +
                "order by test_completion_time";


        logger.info("Starting up in project " + projectId + " and secret " + secretId);
        Config cfg = null;

        try (SecretManagerServiceClient secretManagerServiceClient = SecretManagerServiceClient.create()) {
            var latestSecretVersion = SecretVersionName.of(projectId, secretId, "latest");
            SecretPayload secret = secretManagerServiceClient.accessSecretVersion(latestSecretVersion).getPayload();
            String secretData = secret.getData().toStringUtf8();
            cfg = ConfigFactory.parseString(secretData);
        }
        String dbUrl = cfg.getString("pepperDbUrl");

        PoolingDataSource<PoolableConnection> dataSource = createDataSource(1, dbUrl);

        // static vars are dangerous in CF https://cloud.google.com/functions/docs/bestpractices/tips#functions-tips-scopes-java
        try (Connection conn = dataSource.getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(KIT_QUERY)) {
                stmt.setFetchSize(1000);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        String userGuid = rs.getString("guid");
                        logger.info(userGuid);

                        // spawn threads or other cloud functions in fixed size blocks? Write to a separate queue for worker threads
                        // to grab history?
                    }
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Trouble querying data", e);
        }
    }

    // todo centralize this for stateless db connections.  statics in cloud functions
    // can be shared across invocations, making things like TransactionWrapper.init() hard to predict.
    private PoolingDataSource<PoolableConnection> createDataSource(int maxConnections, String dbUrl) {
        ConnectionFactory connectionFactory = new DriverManagerConnectionFactory(dbUrl, null);
        PoolableConnectionFactory poolableConnectionFactory = new PoolableConnectionFactory(connectionFactory, null);
        poolableConnectionFactory.setDefaultAutoCommit(false);
        GenericObjectPoolConfig poolConfig = new GenericObjectPoolConfig();
        poolConfig.setMaxTotal(maxConnections);
        poolConfig.setTestOnBorrow(false);
        poolConfig.setBlockWhenExhausted(false);
        poolConfig.setTestWhileIdle(true);
        poolConfig.setMinIdle(5);
        poolConfig.setMinEvictableIdleTimeMillis(60000L);
        poolableConnectionFactory.setValidationQueryTimeout(1);
        ObjectPool<PoolableConnection> connectionPool = new GenericObjectPool(poolableConnectionFactory, poolConfig);
        poolableConnectionFactory.setPool(connectionPool);
        PoolingDataSource<PoolableConnection> dataSource = new PoolingDataSource(connectionPool);
        return dataSource;
    }


}
