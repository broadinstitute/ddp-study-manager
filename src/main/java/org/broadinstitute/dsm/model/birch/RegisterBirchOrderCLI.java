package org.broadinstitute.dsm.model.birch;

import java.sql.Connection;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.apache.commons.dbcp2.PoolableConnection;
import org.apache.commons.dbcp2.PoolingDataSource;
import org.broadinstitute.ddp.db.TransactionWrapper;
import org.broadinstitute.dsm.careevolve.Authentication;
import org.broadinstitute.dsm.careevolve.Covid19OrderRegistrar;
import org.broadinstitute.dsm.careevolve.OrderResponse;
import org.broadinstitute.dsm.careevolve.Patient;
import org.broadinstitute.dsm.careevolve.Provider;
import org.broadinstitute.dsm.cf.CFUtil;
import org.broadinstitute.dsm.db.DDPInstance;
import org.broadinstitute.dsm.db.DdpKit;
import org.broadinstitute.dsm.statics.ApplicationConfigConstants;
import org.broadinstitute.dsm.statics.DBConstants;
import org.broadinstitute.dsm.util.ElasticSearchUtil;
import org.elasticsearch.client.RestHighLevelClient;

/**
 * CLI utility for manually placing orders in CareEvolve (and birch)
 * in response to BQMS tickets for kits that have been returned
 * without UPS shipping info.
 */
public class RegisterBirchOrderCLI {

    private static final String USAGE = "args in order are hruid, kitLabel, externalOrderNumber, and collectionTime\n" +
            "for example java -Dconfig.file=... RegisterBirchOrderCLI P1234 TBOS-123 12345678910111213 06/30/2020 15:34\n" +
            "Make sure you are on a broad network\n";
    public static final String DATE_FORMAT = "MM/dd/yyyy hh:mm";


    public static void main(String[] args) {
        if (args == null || args.length == 0 || ("-h".equals(args[0]))) {
            System.out.println(USAGE);
            System.exit(0);
        }
        String participantHruid = args[0];
        String kitLabel = args[1];
        String externalOrderNumber = args[2];
        String collectionTime = args[3];

        Instant collectionDate = null;
        try {
            collectionDate = new SimpleDateFormat(DATE_FORMAT).parse(collectionTime).toInstant();
        } catch(ParseException e) {
            throw new RuntimeException("Could not parse collection time '"  + collectionDate + "'.  Please use " + DATE_FORMAT);
        }

        Config cfg = ConfigFactory.load();
        TransactionWrapper.init(2, cfg.getString("portal.dbUrl"), cfg, true);


        PoolingDataSource<PoolableConnection> dataSource = CFUtil.createDataSource(5, cfg.getString("portal.dbUrl"));

        String careEvolveAccount = cfg.getString(ApplicationConfigConstants.CARE_EVOLVE_ACCOUNT);
        String careEvolveSubscriberKey = cfg.getString(ApplicationConfigConstants.CARE_EVOLVE_SUBSCRIBER_KEY);
        String careEvolveServiceKey = cfg.getString(ApplicationConfigConstants.CARE_EVOLVE_SERVICE_KEY);
        String careEvolveOrderEndpoint = cfg.getString(ApplicationConfigConstants.CARE_EVOLVE_ORDER_ENDPOINT);
        Authentication auth = new Authentication(careEvolveSubscriberKey, careEvolveServiceKey);
        Provider provider = new Provider(cfg.getString(ApplicationConfigConstants.CARE_EVOLVE_PROVIDER_FIRSTNAME),
                cfg.getString(ApplicationConfigConstants.CARE_EVOLVE_PROVIDER_LAST_NAME),
                cfg.getString(ApplicationConfigConstants.CARE_EVOLVE_PROVIDER_NPI));


        DDPInstance ddpInstance = null;

        try (Connection conn = dataSource.getConnection()) {
            ddpInstance = DDPInstance.getDDPInstanceWithRole("testboston", DBConstants.HAS_KIT_REQUEST_ENDPOINTS);
        } catch(SQLException e) {
            throw new RuntimeException("Cannot get instance",e);
        }

        String esUrl = cfg.getString(ApplicationConfigConstants.ES_URL);
        String esUsername = cfg.getString(ApplicationConfigConstants.ES_USERNAME);
        String esPassword = cfg.getString(ApplicationConfigConstants.ES_PASSWORD);
        RestHighLevelClient esClient = null;

        try {
            esClient = ElasticSearchUtil.getClientForElasticsearchCloud(esUrl, esUsername, esPassword);
        } catch(Exception e) {
            throw new RuntimeException("Could not get es client--are you on the VPN?", e);
        }

        Covid19OrderRegistrar orderRegistrar = new Covid19OrderRegistrar(careEvolveOrderEndpoint, careEvolveAccount, provider, 0, 0);

        Map<String, Map<String, Object>> esData = ElasticSearchUtil.getSingleParticipantFromES(ddpInstance.getName(), ddpInstance.getParticipantIndexES(), esClient, participantHruid);

        if (esData.size() == 1) {
            JsonObject participantJsonData = new JsonParser().parse(new Gson().toJson(esData.values().iterator().next())).getAsJsonObject();
            Patient cePatient = Covid19OrderRegistrar.fromElasticData(participantJsonData);
            System.out.println(cePatient);

            OrderResponse orderResponse = orderRegistrar.orderTest(auth, cePatient, kitLabel, externalOrderNumber, collectionDate);

            if (orderResponse.hasError()) {
                throw new RuntimeException("Error placing order: " + orderResponse.getHandle() + " " + orderResponse.getError());
            } else {
                System.out.println("Placed order for " + kitLabel);
            }
            try (Connection conn = dataSource.getConnection()) {
                DdpKit.updateCEOrdered(dataSource.getConnection(), true, kitLabel);
                conn.commit();
            } catch (SQLException e) {
                throw new RuntimeException("Could not update order status.  You may need to manually update DSM to mark ce_ordered", e);
            }
        } else {
            throw new RuntimeException("Could not find es data for " + participantHruid);
        }

        System.exit(0);
    }
}
