package org.broadinstitute.dsm.jobs;

import com.aventrix.jnanoid.jnanoid.NanoIdUtils;
import com.google.api.client.http.HttpStatusCodes;
import com.google.gson.Gson;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.ddp.db.SimpleResult;
import org.broadinstitute.ddp.util.Utility;
import org.broadinstitute.dsm.DSMServer;
import org.broadinstitute.dsm.model.ups.UPSTrackingResponse;
import org.broadinstitute.dsm.statics.DBConstants;
import org.broadinstitute.dsm.util.DDPRequestUtil;
import org.broadinstitute.dsm.util.SecurityUtil;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import static org.broadinstitute.ddp.db.TransactionWrapper.inTransaction;

public class UPSTrackingJob implements Job {

    private static final Logger logger = LoggerFactory.getLogger(UPSTrackingJob.class);
    private static final String SQL_SELECT_KITS = "SELECT * FROM ddp_kit kit LEFT JOIN ddp_kit_request req ON (kit.dsm_kit_request_id = req.dsm_kit_request_id) WHERE req.ddp_instance_id = ?";
    static String upsTrackingEndpoint = "https://wwwcie.ups.com/track/v1/details/";//todo pegah should be changed for prod and moved to vault
    static String upsAccessCode = "";
    static String upsUserName = "";
    String upsPassword = "";

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        //        String realm = getRealmsWithRole();
        Map<String, Set<String>> ids = getResultSet("15");
        ArrayList<String> trackingIds = (ArrayList<String>) ids.get("shipping");
        ArrayList<String> returnTrackingIds = (ArrayList<String>) ids.get("return");
        for (String trackingId : trackingIds) {
            String transId = NanoIdUtils.randomNanoId(
                    NanoIdUtils.DEFAULT_NUMBER_GENERATOR, "1234567890QWERTYUIOPASDFGHJKLZXCVBNM".toCharArray(), 32);
            String inquiryNumber = trackingId;
            String transSrc = "TestTracking";
            String sendRequest = upsTrackingEndpoint + inquiryNumber;
            Map<String, String> headers = new HashMap<>();
            headers.put("transId", transId);
            headers.put("transSrc", transSrc);
            headers.put("Username", DSMServer.UPS_USERNAME);
            headers.put("AccessLicenseNumber", DSMServer.UPS_ACCESSKEY);
            headers.put("Content-Type", "application/json");
            headers.put("Accept", "application/json");
            try {
                UPSTrackingResponse response = DDPRequestUtil.getResponseObjectWithCustomHeader(UPSTrackingResponse.class, sendRequest, "UPS Tracking Test " + inquiryNumber, headers);
                logger.info("got response back: " + response);

            }
            catch (IOException e) {
                throw new RuntimeException("couldn't get response from ups tracking ", e);
            }
        }
    }

    public static void testMethod() {
        Map<String, Set<String>> ids = getResultSet("15");
        Set<String> trackingIds = (HashSet<String>) ids.get("shipping");
        Set<String> returnTrackingIds = (HashSet<String>) ids.get("return");
        for (String trackingId : returnTrackingIds) {
            String transId = NanoIdUtils.randomNanoId(
                    NanoIdUtils.DEFAULT_NUMBER_GENERATOR, "1234567890QWERTYUIOPASDFGHJKLZXCVBNM".toCharArray(), 32);
            String inquiryNumber = trackingId;
            String transSrc = "TestTracking";
            String sendRequest = upsTrackingEndpoint + inquiryNumber;
            Map<String, String> headers = new HashMap<>();
            headers.put("transId", transId);
            headers.put("transSrc", transSrc);
            headers.put("Username", upsUserName);
            headers.put("AccessLicenseNumber", upsAccessCode);
            headers.put("Content-Type", "application/json");
            headers.put("Accept", "application/json");
            try {
                UPSTrackingResponse response = DDPRequestUtil.getResponseObjectWithCustomHeader(UPSTrackingResponse.class, sendRequest, "UPS Tracking Test " + inquiryNumber, headers);
                logger.info("got response back: " + response);

            }
            catch (IOException e) {
                throw new RuntimeException("couldn't get response from ups tracking ", e);
            }
        }
    }

    public static Map<String, Set<String>> getResultSet(String realm) {
        SimpleResult result = inTransaction((conn) -> {
            SimpleResult dbVals = new SimpleResult();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_SELECT_KITS)) {
                stmt.setString(1, realm);
                try (ResultSet rs = stmt.executeQuery()) {
                    Map<String, Set<String>> results = getIdsFromResultSet(rs);
                    dbVals.resultValue = results;
                    return dbVals;
                }
                catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            catch (Exception e) {
                dbVals.resultException = e;
            }
            return dbVals;
        });
        if (result.resultException != null) {
            throw new RuntimeException(result.resultException);
        }
        return (Map<String, Set<String>>) result.resultValue;
    }

    public static Map<String, Set<String>> getIdsFromResultSet(ResultSet rs) {
        Set<String> returnTrackingIds = new HashSet<>();
        Set<String> trackingIds = new HashSet<>();
        try {
            while (rs.next()) {
                if (StringUtils.isNotBlank(rs.getString("kit.tracking_to_id"))) {
                    trackingIds.add(rs.getString("kit.tracking_to_id"));
                }
                if (StringUtils.isNotBlank(rs.getString("kit.tracking_return_id"))) {
                    returnTrackingIds.add(rs.getString("kit.tracking_return_id"));
                }
            }
        }
        catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
        Map<String, Set<String>> results = new HashMap<>();
        results.put("shipping", trackingIds);
        results.put("return", returnTrackingIds);
        return results;
    }


}
