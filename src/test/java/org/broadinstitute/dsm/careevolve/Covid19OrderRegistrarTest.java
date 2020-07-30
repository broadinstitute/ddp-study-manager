package org.broadinstitute.dsm.careevolve;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.ddp.db.TransactionWrapper;
import org.broadinstitute.dsm.TestHelper;
import org.broadinstitute.dsm.db.DDPInstance;
import org.broadinstitute.dsm.model.ParticipantWrapper;
import org.broadinstitute.dsm.statics.ApplicationConfigConstants;
import org.broadinstitute.dsm.statics.DBConstants;
import org.broadinstitute.dsm.statics.RoutePath;
import org.broadinstitute.dsm.util.ElasticSearchUtil;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Covid19OrderRegistrarTest {

    private static final Logger logger = LoggerFactory.getLogger(Covid19OrderRegistrarTest.class);

    private static Authentication auth;

    private static String careEvolveOrderEndpoint;

    private static String  careEvolveAccount;

    private static Config cfg;

    @BeforeClass
    public static void beforeClass() throws Exception {
        cfg = ConfigFactory.load();
        cfg = cfg.withFallback(ConfigFactory.parseFile(new File("config/test-config.conf")));
        TransactionWrapper.init(20, cfg.getString("portal.dbUrl"), cfg, true);
        //startMockServer();
        //setupUtils();

        cfg = ConfigFactory.load().withFallback(ConfigFactory.parseFile(new File("config/ellkay.conf")));
        careEvolveAccount = cfg.getString(ApplicationConfigConstants.CARE_EVOLVE_ACCOUNT);
        String careEvolveSubscriberKey = cfg.getString(ApplicationConfigConstants.CARE_EVOLVE_SUBSCRIBER_KEY);
        String careEvolveServiceKey = cfg.getString(ApplicationConfigConstants.CARE_EVOLVE_SERVICE_KEY);
        careEvolveOrderEndpoint = cfg.getString(ApplicationConfigConstants.CARE_EVOLVE_ORDER_ENDPOINT);
        auth = new Authentication(careEvolveSubscriberKey, careEvolveServiceKey);
    }

    @Test
    public void testSendCareEvoloveOrder() throws Exception {

        Covid19OrderRegistrar orderRegistrar = new Covid19OrderRegistrar(careEvolveOrderEndpoint);

        // todo move provider to config file
        Provider provider = new Provider("Lisa", "Cosimi", "1154436111");
        String pepperUserGuid = "1234";
        String pepperKitGuid =  "789";



        List<AOE> aoes = AOE.forTestBoston(pepperUserGuid, pepperKitGuid);
        Address address = new Address("75 Ames St.",null,"Cambridge","MA","02421");
        Patient testPatient = new Patient("123","foo","bar","1901-01-01","Other","Other",
                "other",address);

        Message message = new Message(new Order(careEvolveAccount, testPatient, provider, aoes), "GBF2929101");

        OrderResponse orderResponse = orderRegistrar.orderTest(auth, message);
        Gson gson = new GsonBuilder().serializeNulls().setPrettyPrinting().create();

        logger.info("Order returned " + gson.toJson(orderResponse));

        Assert.assertNotNull(orderResponse);
        Assert.assertNull(orderResponse.getError());
        Assert.assertTrue(StringUtils.isNotBlank(orderResponse.getHandle()));


    }

    @Test
    public void testOrdereForParticipant() throws Exception {
        Covid19OrderRegistrar orderRegistrar = new Covid19OrderRegistrar(careEvolveOrderEndpoint);

        orderRegistrar.orderTest("X2LDCNFX16KUYX5Y74LW");

    }
}
