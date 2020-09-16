package org.broadinstitute.dsm.jobs;

import com.aventrix.jnanoid.jnanoid.NanoIdUtils;
import com.google.api.client.http.HttpStatusCodes;
import com.google.gson.Gson;
import org.broadinstitute.ddp.db.SimpleResult;
import org.broadinstitute.ddp.util.Utility;
import org.broadinstitute.dsm.statics.DBConstants;
import org.broadinstitute.dsm.util.SecurityUtil;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Response;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.broadinstitute.ddp.db.TransactionWrapper.inTransaction;

public class UPSTrackingJob implements Job {

    private static final Logger logger = LoggerFactory.getLogger(UPSTrackingJob.class);
    private static final String SQL_SELECT_KITS = "SELECT * FROM ddp_kit kit LEFT JOIN ddp_kit_request req ON (kit.dsm_kit_request_id = req.dsm_kit_request_id) AND kit.ddp_instance_id = ?";
    String upsTrackingEndpoint="https://wwwcie.ups.com/track/v1/details/";//todo pegah should be changed for prod and moved to vault
    String upsAccessCode = "";
    String upsUserName = "";
    String upsPassword = "";

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        ResultSet rs = getResultSet("15");
        ArrayList<String> trackingIds = (ArrayList<String>) getTrackingIdsFromResultSet(rs);
        ArrayList<String> returnTrackingIds = (ArrayList<String>) getReturnIdsFromResultSet(rs);
        for(String trackingId: trackingIds){
            String transId = NanoIdUtils.randomNanoId(
                    NanoIdUtils.DEFAULT_NUMBER_GENERATOR, "1234567890QWERTYUIOPASDFGHJKLZXCVBNM".toCharArray(), 32);
            String inquiryNumber = trackingId;
            String transSrc = "TestTracking";
        }
    }

    public static ResultSet getResultSet(String realm) {
        SimpleResult result = inTransaction((conn) -> {
            SimpleResult dbVals = new SimpleResult();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_SELECT_KITS)) {
                stmt.setString(1, realm);
                try (ResultSet rs = stmt.executeQuery()) {
                    dbVals.resultValue = rs;
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
        return ((ResultSet) result.resultValue);
    }

    public static List<String> getReturnIdsFromResultSet(ResultSet rs) {
        ArrayList<String> trackingIds = new ArrayList<>();
        try {
            while (rs.next()) {
                trackingIds.add(rs.getString(DBConstants.TRACKING_RETURN_ID));
            }
        }
        catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
        return trackingIds;
    }

    public static List<String> getTrackingIdsFromResultSet(ResultSet rs) {
        ArrayList<String> returnTrackingIds = new ArrayList<>();
        try {
            while (rs.next()) {
                returnTrackingIds.add(rs.getString(DBConstants.TRACKING_RETURN_ID));
            }
        }
        catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
        return returnTrackingIds;
    }


}
