package org.broadinstitute.dsm;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import lombok.NonNull;
import org.broadinstitute.dsm.statics.ApplicationConfigConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigUtil {

    private static final Logger logger = LoggerFactory.getLogger(DSMServer.class);

    private static final String VAULT_DOT_CONF = "vault.conf";

    private static final String GAE_DEPLOY_DIR = "appengine/deploy";

    private static Map<String, JsonElement> ddpConfigurationLookup = new HashMap<>();

    public static Config cfg;

    static {
        cfg = loadConfig();
    }

    private static Config loadConfig() {
        Config cfg = ConfigFactory.load();
        //secrets from vault in a config file
        File vaultConfigInCwd = new File(VAULT_DOT_CONF);
        File vaultConfigInDeployDir = new File(GAE_DEPLOY_DIR, VAULT_DOT_CONF);
        File vaultConfig = vaultConfigInCwd.exists() ? vaultConfigInCwd : vaultConfigInDeployDir;
        logger.info("Reading config values from " + vaultConfig.getAbsolutePath());
        cfg = cfg.withFallback(ConfigFactory.parseFile(vaultConfig));

        setupDDPConfigurationLookup(cfg.getString(ApplicationConfigConstants.DDP));
        setupExternalShipperLookup(cfg.getString(ApplicationConfigConstants.EXTERNAL_SHIPPER));
        return cfg;
    }

    private static void setupExternalShipperLookup(@NonNull String externalSipperConf) {
        JsonArray array = (JsonArray) (new JsonParser().parse(externalSipperConf));
        for (JsonElement ddpInfo : array) {
            if (ddpInfo.isJsonObject()) {
                ddpConfigurationLookup.put(ddpInfo.getAsJsonObject().get(ApplicationConfigConstants.SHIPPER_NAME).getAsString().toLowerCase(), ddpInfo);
            }
        }
    }

    private static void setupDDPConfigurationLookup(@NonNull String ddpConf) {
        JsonArray array = (JsonArray) (new JsonParser().parse(ddpConf));
        for (JsonElement ddpInfo : array) {
            if (ddpInfo.isJsonObject()) {
                ddpConfigurationLookup.put(ddpInfo.getAsJsonObject().get(ApplicationConfigConstants.INSTANCE_NAME).getAsString().toLowerCase(), ddpInfo);
            }
        }
    }

    public static String getDDPTokenSecret(@NonNull String instanceName) {
        JsonElement jsonElement = ddpConfigurationLookup.get(instanceName.toLowerCase());
        if (jsonElement != null && jsonElement.getAsJsonObject().has(ApplicationConfigConstants.TOKEN_SECRET)) {
            return jsonElement.getAsJsonObject().get(ApplicationConfigConstants.TOKEN_SECRET).getAsString();
        }
        return null;
    }

    public static String getDDPEasypostApiKey(@NonNull String instanceName) {
        JsonElement jsonElement = ddpConfigurationLookup.get(instanceName.toLowerCase());
        if (jsonElement != null) {
            return jsonElement.getAsJsonObject().get(ApplicationConfigConstants.EASYPOST_API_KEY).getAsString();
        }
        return null;
    }

    public static String getShipperApiKey(@NonNull String shipperName) {
        JsonElement jsonElement = ddpConfigurationLookup.get(shipperName.toLowerCase());
        if (jsonElement != null) {
            return jsonElement.getAsJsonObject().get(ApplicationConfigConstants.API_KEY).getAsString();
        }
        return null;
    }

    public static String getShipperBaseUrl(@NonNull String shipperName) {
        JsonElement jsonElement = ddpConfigurationLookup.get(shipperName.toLowerCase());
        if (jsonElement != null) {
            return jsonElement.getAsJsonObject().get(ApplicationConfigConstants.BASE_URL).getAsString();
        }
        return null;
    }

    public static String getShipperClassName(@NonNull String shipperName) {
        JsonElement jsonElement = ddpConfigurationLookup.get(shipperName.toLowerCase());
        if (jsonElement != null) {
            return jsonElement.getAsJsonObject().get(ApplicationConfigConstants.CLASS_NAME).getAsString();
        }
        return null;
    }

    public static boolean isTestShipment(@NonNull String shipperName) {
        JsonElement jsonElement = ddpConfigurationLookup.get(shipperName.toLowerCase());
        if (jsonElement != null) {
            return jsonElement.getAsJsonObject().get(ApplicationConfigConstants.TEST).getAsBoolean();
        }
        return true;
    }
}
