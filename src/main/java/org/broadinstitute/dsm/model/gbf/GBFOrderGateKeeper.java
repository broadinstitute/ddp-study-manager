package org.broadinstitute.dsm.model.gbf;

import java.net.MalformedURLException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collection;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sendgrid.SendGrid;
import com.sendgrid.SendGridException;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.broadinstitute.ddp.db.TransactionWrapper;
import org.broadinstitute.dsm.DSMServer;
import org.broadinstitute.dsm.careevolve.Authentication;
import org.broadinstitute.dsm.careevolve.Provider;
import org.broadinstitute.dsm.cf.CFUtil;
import org.broadinstitute.dsm.db.DDPInstance;
import org.broadinstitute.dsm.model.KitRequestSettings;
import org.broadinstitute.dsm.statics.ApplicationConfigConstants;
import org.broadinstitute.dsm.util.ElasticSearchUtil;
import org.elasticsearch.client.RestHighLevelClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Uses a {@link GBFOrderTransmitter} and {@link GBFOrderFinder}
 * to find and transmit orders to GBF for a given
 * DDP instance
 */
public class GBFOrderGateKeeper {

    private static final Logger logger = LoggerFactory.getLogger(GBFOrderFinder.class);
    public static final String GBF = "gbf";

    private final GBFOrderFinder orderFinder;
    private final GBFOrderTransmitter transmitter;
    private final String ddpInstanceName;
    private final String sendgridAPIKey;
    private final String emailSummaryAddress;
    private final String emailFrom;
    private static final String UPDATE_TRANSMISSION_DATE = "update ddp_kit_request req set external_order_status = 'ORDERED', req.order_transmitted_at = ? where req.external_order_number = ?";

    public GBFOrderGateKeeper(GBFOrderFinder orderFinder, GBFOrderTransmitter transmitter, String ddpInstanceName, String sendgridAPIKey,
                              String emailSummaryAddress, String emailFrom) {
        this.orderFinder = orderFinder;
        this.transmitter = transmitter;
        this.ddpInstanceName = ddpInstanceName;
        this.sendgridAPIKey = sendgridAPIKey;
        this.emailSummaryAddress = emailSummaryAddress;
        this.emailFrom = emailFrom;
    }


    public void sendPendingOrders(Connection conn) {
        Collection<SimpleKitOrder> kitsToOrder = orderFinder.findKitsToOrder(ddpInstanceName, conn);
        logger.info("About to order {} kits", kitsToOrder.size());
        for (SimpleKitOrder simpleKitOrder : kitsToOrder) {
            logger.info("About to order {} for {}", simpleKitOrder.getExternalKitOrderNumber(), simpleKitOrder.getParticipantGuid());
            Response orderResponse = transmitter.orderKit(simpleKitOrder.getRecipientAddress(), simpleKitOrder.getExternalKitName(), simpleKitOrder.getExternalKitOrderNumber(), simpleKitOrder.getParticipantGuid());

            if (orderResponse.isSuccess()) {
                // save result in separate transaction so that we minimize the chance
                // of large scale order duplication
                TransactionWrapper.inTransaction(markOrderAsTransmittedTxn -> {
                    try (PreparedStatement stmt = markOrderAsTransmittedTxn.prepareStatement(UPDATE_TRANSMISSION_DATE)) {
                        stmt.setTimestamp(1, Timestamp.from(Instant.now()));
                        stmt.setString(2, simpleKitOrder.getExternalKitOrderNumber());
                        int numRowsUpdated = stmt.executeUpdate();

                        // putting 10 things in a kit seems crazy?
                        if (numRowsUpdated > 10) {
                            throw new RuntimeException("Updated " + numRowsUpdated + " for order " + simpleKitOrder.getExternalKitOrderNumber() + ".  That seems like a lot?");
                        }
                    } catch (SQLException e) {
                        throw new RuntimeException("Order " + simpleKitOrder.getExternalKitOrderNumber() + " has been ordered, but we were unable to update the database.", e);
                    }
                    return null;
                });

            } else {
                throw new RuntimeException("Transmission of order " + simpleKitOrder.getExternalKitOrderNumber() + " failed with " + orderResponse.getErrorMessage());
            }
        }

        // sendgrid notification of kits sent
        SendGrid sendGrid = new SendGrid(sendgridAPIKey);

        String notificationText = kitsToOrder.size() + " kits ordered for " + ddpInstanceName;
        SendGrid.Response emailResponse = null;

        StringBuilder kitsSentSummary = new StringBuilder();
        kitsSentSummary.append(notificationText + "\n\n");
        kitsSentSummary.append("short id,kit order number,scheduled date\n");
        for (SimpleKitOrder simpleKitOrder : kitsToOrder) {
            kitsSentSummary.append(simpleKitOrder.getShortId()).append(",")
                    .append(simpleKitOrder.getExternalKitOrderNumber()).append(",")
                    .append(simpleKitOrder.getScheduledAt()).append("\n");
        }

        try {
           emailResponse = sendGrid.send(new SendGrid.Email().addTo(emailSummaryAddress).setFrom(emailFrom).setSubject(notificationText).setText(kitsSentSummary.toString()));
        } catch (SendGridException e) {
            logger.error("Could not email summary to " + emailSummaryAddress,e);
        }

        if (!emailResponse.getStatus()) {
            logger.error("Could not email summary to " + emailSummaryAddress + " due to " + emailResponse.getMessage());
        }
    }


    public static void main(String[] args) {

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
            String carrierService = "3rd Day Air Residential";
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
}