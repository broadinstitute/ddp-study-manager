package org.broadinstitute.dsm.cf;

import static org.broadinstitute.dsm.model.gbf.GBFOrderGateKeeper.GBF;

import java.net.MalformedURLException;
import java.util.Collection;

import com.google.cloud.functions.BackgroundFunction;
import com.google.cloud.functions.Context;
import com.typesafe.config.Config;
import org.broadinstitute.ddp.db.TransactionWrapper;
import org.broadinstitute.dsm.DSMServer;
import org.broadinstitute.dsm.db.DDPInstance;
import org.broadinstitute.dsm.model.KitRequestSettings;
import org.broadinstitute.dsm.model.gbf.GBFOrderFinder;
import org.broadinstitute.dsm.model.gbf.GBFOrderGateKeeper;
import org.broadinstitute.dsm.model.gbf.GBFOrderTransmitter;
import org.broadinstitute.dsm.statics.ApplicationConfigConstants;
import org.broadinstitute.dsm.util.ElasticSearchUtil;
import org.elasticsearch.client.RestHighLevelClient;

/**
 * Sends orders to GBF using a filter for longitudinal kits such that
 * a kit will only be ordered for a participant if it is their first kit or
 * they have returned a kit within 45 days.
 */
public class TestBostonKitOrderer implements BackgroundFunction<TestBostonKitOrderer.OrderRequest> {

    @Override
    public void accept(OrderRequest orderRequest, Context context) throws Exception {
        try {
            String ddpInstanceName = System.getenv("DDP_INSTANCE");
            Config cfg = CFUtil.loadConfig();
            RestHighLevelClient esClient = null;
            DSMServer.setupExternalShipperLookup(cfg.getString(ApplicationConfigConstants.EXTERNAL_SHIPPER));
            String dbUrl = cfg.getString(ApplicationConfigConstants.DSM_DB_URL);
            String esUser = cfg.getString(ApplicationConfigConstants.ES_USERNAME);
            String esPassword = cfg.getString(ApplicationConfigConstants.ES_PASSWORD);
            String esUrl = cfg.getString(ApplicationConfigConstants.ES_URL);
            String gbfUrl = DSMServer.getBaseUrl(GBF);
            String gbfApiKey = DSMServer.getApiKey(GBF);
            String carrierService = System.getenv("SHIPPING_RATE");
            String externalClientId = null;
            String sendGridApiKey = cfg.getString(ApplicationConfigConstants.EMAIL_KEY);
            String emailSummaryTo = System.getenv("EMAIL_TO");
            String emailSummaryFrom = System.getenv("EMAIL_FROM");

            TransactionWrapper.init(5, dbUrl,cfg, true);
            try {
                esClient = ElasticSearchUtil.getClientForElasticsearchCloud(esUrl, esUser, esPassword);
            } catch (MalformedURLException e) {
                throw new RuntimeException("Could not initialize es client", e);
            }

            DDPInstance ddpInstance = DDPInstance.getDDPInstance(ddpInstanceName);
            Collection<KitRequestSettings> kitRequestSettings = KitRequestSettings.getKitRequestSettings(ddpInstance.getDdpInstanceId()).values();

            for (KitRequestSettings kitRequestSetting : kitRequestSettings) {
                if (GBF.equals(kitRequestSetting.getExternalShipper())) {
                    externalClientId = kitRequestSetting.getExternalClientId();
                }
            }

            GBFOrderFinder orderFinder = new GBFOrderFinder(45, 10000, esClient, "participants_structured.testboston.testboston");

            GBFOrderTransmitter transmitter = new GBFOrderTransmitter(false, gbfUrl, gbfApiKey, 3, 2000, carrierService, externalClientId);
            GBFOrderGateKeeper keeper = new GBFOrderGateKeeper(orderFinder, transmitter, ddpInstanceName, sendGridApiKey, emailSummaryTo, emailSummaryFrom);


            TransactionWrapper.inTransaction(conn -> {
                keeper.sendPendingOrders(conn);
                return null;
            });
        } catch (Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        } finally {
            System.exit(0);
        }
    }

    public static class OrderRequest {

    }
}
