package org.broadinstitute.dsm.route;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.ddp.db.SimpleResult;
import org.broadinstitute.dsm.db.DDPInstance;
import org.broadinstitute.dsm.db.KitRequestShipping;
import org.broadinstitute.dsm.statics.RequestParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;
import spark.Route;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.broadinstitute.ddp.db.TransactionWrapper.inTransaction;

public class MercuryDummyKitEndpoint implements Route {
    private static final String DUMMY_KIT_TYPE_NAME = "DUMMY_KIT_TYPE";
    private static final String DUMMY_REALM_NAME = "DUMMY_KIT_REALM";
    private static final Logger logger = LoggerFactory.getLogger(MercuryDummyKitEndpoint.class);
    private static final String SELECT_KIT_TYPE = "Select value from bookmark where instance= ?";
    @Override
    public Object handle(Request request, Response response) throws Exception {
       logger.info("Got Mercury Test request");
        String kitLabel = request.params(RequestParameter.LABEL);
        if (StringUtils.isBlank(kitLabel)) {
            throw new RuntimeException("Please include a kit label as a path parameter");
        }
        try{
        DDPInstance mockDdpInstance = DDPInstance.getDDPInstance(DUMMY_REALM_NAME);
        String mercuryKitRequestId = "MERCURY_"+ KitRequestShipping.createRandom(20);
        String kitTypeId = "";
            SimpleResult results = inTransaction((conn) -> {
                SimpleResult dbVals = new SimpleResult();
                try (PreparedStatement insertKitRequest = conn.prepareStatement(SELECT_KIT_TYPE)) {
                    insertKitRequest.setString(1, mockDdpInstance.getDdpInstanceId());
                    try (ResultSet rs = insertKitRequest.executeQuery()) {
                        if (rs.next()) {
                            dbVals.resultValue = rs.getString("value");
                        }
                    }
                    catch (Exception e) {
                        throw new RuntimeException("Error getting id of new kit request ", e);
                    }
                }
                catch (SQLException ex) {
                    dbVals.resultException = ex;
                }
                return dbVals;
            });

            if (results.resultException != null) {
                throw new RuntimeException("Error getting the kit type from bookmark table ", results.resultException);
            }
            kitTypeId = (String) results.resultValue;
            logger.info("Found kit type "+kitTypeId);
//if instance not null
            KitRequestShipping.writeRequest(mockDdpInstance.getDdpInstanceId(),mercuryKitRequestId, kitTypeId,
            "DUMMY_PARTICIPANT", "", "",
                    "SYSTEM", "", null, null, false, null);
        }
    }
}
