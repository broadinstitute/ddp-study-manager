package org.broadinstitute.dsm.route;

import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.ddp.db.SimpleResult;
import org.broadinstitute.ddp.db.TransactionWrapper;
import org.broadinstitute.ddp.handlers.util.Result;
import org.broadinstitute.dsm.db.DDPInstance;
import org.broadinstitute.dsm.db.KitRequestShipping;
import org.broadinstitute.dsm.statics.ApplicationConfigConstants;
import org.broadinstitute.dsm.statics.RequestParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;
import spark.Route;

import java.sql.*;

import static org.broadinstitute.ddp.db.TransactionWrapper.inTransaction;

public class MercuryDummyKitEndpoint implements Route {
    private static final String DUMMY_KIT_TYPE_NAME = "DUMMY_KIT_TYPE";
    private static final String DUMMY_REALM_NAME = "DUMMY_KIT_REALM";
    private static final Logger logger = LoggerFactory.getLogger(MercuryDummyKitEndpoint.class);
    private static final String SELECT_KIT_TYPE = "Select `value` from bookmark where instance= ?";
    private static final String INSERT_DUMMY_KIT = "INSERT INTO ddp_kit (dsm_kit_request_id, kit_label) VALUES (?,?)";
    private static String ddpParticipantId = "I211Q8BK5ZJHNG43DJVQ";
    private static String participantCollaboratorId= "OSProject_P8EQ67";
    private static String collaboratorSampleId = "OSProject_P8EQ67_SALIVA";

    @Override
    public Object handle(Request request, Response response) throws Exception {
        logger.info("Got Mercury Test request");
        String kitLabel = request.params(RequestParameter.LABEL);
        if (StringUtils.isBlank(kitLabel)) {
            response.status(400);// return bad request
            return new Result(400, "Please include a kit label as a path parameter");
        }
        DDPInstance mockDdpInstance = DDPInstance.getDDPInstance(DUMMY_REALM_NAME);
        String mercuryKitRequestId = "MERCURY_" + KitRequestShipping.createRandom(20);
        int kitTypeId;
        SimpleResult results = inTransaction((conn) -> {
            SimpleResult dbVals = new SimpleResult();
            try (PreparedStatement insertKitRequest = conn.prepareStatement(SELECT_KIT_TYPE)) {
                insertKitRequest.setString(1, mockDdpInstance.getDdpInstanceId());
                try (ResultSet rs = insertKitRequest.executeQuery()) {
                    if (rs.next()) {
                        dbVals.resultValue = rs.getInt("value");
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
        kitTypeId = (int) results.resultValue;
        logger.info("Found kit type for Mercury Dummy Endpoint " + kitTypeId);
        
        //if instance not null
        writeRequest(mockDdpInstance.getDdpInstanceId(), mercuryKitRequestId, kitTypeId,
                ddpParticipantId, participantCollaboratorId, collaboratorSampleId,
                "SYSTEM", kitLabel);

        return new Result(200);
    }

    static String writeRequest(@NonNull String instanceId, @NonNull String ddpKitRequestId, @NonNull int kitTypeId,
                               @NonNull String ddpParticipantId, String collaboratorPatientId, String collaboratorSampleId,
                               @NonNull String createdBy, String kitLabel) {
        SimpleResult results = inTransaction((conn) -> {
            SimpleResult dbVals = new SimpleResult(0);
            try (PreparedStatement insertKitRequest = conn.prepareStatement(TransactionWrapper.getSqlFromConfig(ApplicationConfigConstants.INSERT_KIT_REQUEST), Statement.RETURN_GENERATED_KEYS)) {
                insertKitRequest.setString(1, instanceId);
                insertKitRequest.setString(2, ddpKitRequestId);
                insertKitRequest.setInt(3, kitTypeId);
                insertKitRequest.setString(4, ddpParticipantId);
                insertKitRequest.setObject(5, collaboratorPatientId);
                insertKitRequest.setObject(6, collaboratorSampleId);
                insertKitRequest.setNull(7, Types.VARCHAR); //ddp_label or shipping_id
                insertKitRequest.setString(8, createdBy);
                insertKitRequest.setLong(9, System.currentTimeMillis());
                insertKitRequest.setNull(10, Types.VARCHAR); //external_order_number
                insertKitRequest.setNull(11, Types.VARCHAR); //upload reason
                insertKitRequest.executeUpdate();
                try (ResultSet rs = insertKitRequest.getGeneratedKeys()) {
                    if (rs.next()) {
                        dbVals.resultValue = rs.getString(1);
                    }
                }
                catch (Exception e) {
                    throw new RuntimeException("Error getting id of new kit request ", e);
                }
            }
            catch (SQLException ex) {
                dbVals.resultException = ex;
            }
            if (dbVals.resultException == null && dbVals.resultValue != null) {
                writeNewKit(conn, (String) dbVals.resultValue, kitLabel);
            }
            return dbVals;
        });

        if (results.resultException != null) {
            throw new RuntimeException("Error adding kit request  w/ ddpKitRequestId " + ddpKitRequestId, results.resultException);
        }

        logger.info("Added kitRequest w/ ddpKitRequestId " + ddpKitRequestId);
        return (String) results.resultValue;
    }

    private static SimpleResult writeNewKit(Connection conn, String kitRequestId, String kiLabel) {
        SimpleResult dbVals = new SimpleResult();
        try (PreparedStatement insertKit = conn.prepareStatement(INSERT_DUMMY_KIT)) {
            insertKit.setString(1, kitRequestId);
            insertKit.setString(2, kiLabel);
            insertKit.executeUpdate();
        }
        catch (SQLException e) {
            dbVals.resultException = e;
        }
        return dbVals;
    }
}
