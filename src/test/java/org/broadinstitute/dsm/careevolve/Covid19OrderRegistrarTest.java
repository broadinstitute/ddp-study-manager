package org.broadinstitute.dsm.careevolve;

import java.io.File;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.ddp.db.TransactionWrapper;
import org.broadinstitute.dsm.statics.ApplicationConfigConstants;
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
    public void testOrderForParticipant() throws Exception {
        Covid19OrderRegistrar orderRegistrar = new Covid19OrderRegistrar(careEvolveOrderEndpoint, careEvolveAccount);

        orderRegistrar.orderTest(auth,"PKDG8J","GBF1213","kit124");

    }
}
