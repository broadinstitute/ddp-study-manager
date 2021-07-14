package org.broadinstitute.dsm.route;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.ddp.db.SimpleResult;
import org.broadinstitute.ddp.handlers.util.Result;
import org.broadinstitute.dsm.db.DDPInstance;
import org.broadinstitute.dsm.db.KitRequestShipping;
import org.broadinstitute.dsm.statics.DBConstants;
import org.broadinstitute.dsm.statics.RequestParameter;
import org.broadinstitute.dsm.util.DBUtil;
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
    private static final String USER_ID = "74";
    private static final Logger logger = LoggerFactory.getLogger(MercuryDummyKitEndpoint.class);
    private static final String SQL_UPDATE_DUMMY_KIT = "UPDATE ddp_kit SET kit_label = ? where dsm_kit_request_id = ?";
    private static final String SQL_SELECT_RANDOM_PT = "SELECT ddp_participant_id FROM ddp_kit_request where ddp_instance_id = ?  ORDER BY RAND() LIMIT 1";

    @Override
    public Object handle(Request request, Response response) throws Exception {
        logger.info("Got Mercury Test request");
        String kitLabel = request.params(RequestParameter.LABEL);
        if (StringUtils.isBlank(kitLabel)) {
            response.status(400);// return bad request
            logger.error("Bad request from Mercury! Should include a kitlabel");
            return new Result(400, "Please include a kit label as a path parameter");
        }
        logger.info("Found kitlabel " + kitLabel + " in Mercury request");
        int ddpInstanceId = (int) DBUtil.getBookmark(DUMMY_REALM_NAME);
        logger.info("Found ddp instance id for mock test " + ddpInstanceId);
        DDPInstance mockDdpInstance = DDPInstance.getDDPInstanceById(ddpInstanceId);
        if (mockDdpInstance != null) {
            logger.info("Found mockDdpInstance " + mockDdpInstance.getName());
            String mercuryKitRequestId = "MERCURY_" + KitRequestShipping.createRandom(20);
            int kitTypeId = (int) DBUtil.getBookmark(DUMMY_KIT_TYPE_NAME);
            logger.info("Found kit type for Mercury Dummy Endpoint " + kitTypeId);

            String ddpParticipantId = getRandomParticipantIdForStudy(mockDdpInstance.getDdpInstanceId());
            String participantCollaboratorId = KitRequestShipping.getCollaboratorParticipantId(mockDdpInstance.getBaseUrl(), mockDdpInstance.getDdpInstanceId(), mockDdpInstance.isMigratedDDP(),
                    mockDdpInstance.getCollaboratorIdPrefix(), ddpParticipantId, "", null);
            String collaboratorSampleId = getCollaboratorSampleId(kitTypeId, participantCollaboratorId);
            if (ddpParticipantId != null) {
                //if instance not null
                String dsmKitRequestId = KitRequestShipping.writeRequest(mockDdpInstance.getDdpInstanceId(), mercuryKitRequestId, kitTypeId,
                        ddpParticipantId, participantCollaboratorId, collaboratorSampleId,
                        USER_ID, "", "", "", false, "");
                updateKitLabel(kitLabel, dsmKitRequestId);
            }
            logger.info("Returning 200 to Mercury");
            return new Result(200);
        }
        logger.error("Returning 500 to Mercury");
        return new Result(500);
    }

    public static void updateKitLabel(String kitLabel, String dsmKitRequestId) {
        SimpleResult results = inTransaction((conn) -> {
            SimpleResult dbVals = new SimpleResult();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_UPDATE_DUMMY_KIT)) {
                stmt.setString(1, kitLabel);
                stmt.setString(2, dsmKitRequestId);
                int result = stmt.executeUpdate();
                if (result == 1) {
                    logger.info("Updated dummy kit, set KitLabel " + kitLabel + " for kit with dsmKitRequestId " + dsmKitRequestId);
                }
                else {
                    throw new RuntimeException("Error updating kit  label for " + dsmKitRequestId + " updated " + result + " rows");
                }
            }
            catch (SQLException e) {
                dbVals.resultException = e;
            }
            return dbVals;
        });
        if (results.resultException != null) {
            throw new RuntimeException("Error updating kit  label for " + dsmKitRequestId, results.resultException);
        }
    }

    public static String getRandomParticipantIdForStudy(String ddpInstanceId) {
        SimpleResult results = inTransaction((conn) -> {
            SimpleResult dbVals = new SimpleResult();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_SELECT_RANDOM_PT)) {
                stmt.setString(1, ddpInstanceId);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    dbVals.resultValue = rs.getString(DBConstants.DDP_PARTICIPANT_ID);
                }
            }
            catch (SQLException e) {
                dbVals.resultException = e;
            }
            return dbVals;
        });
        if (results.resultException != null) {
            throw new RuntimeException("Problem getting a random participant id for instance id " + ddpInstanceId, results.resultException);
        }
        if (results.resultValue != null) {
            return (String) results.resultValue;
        }
        return null;
    }

    public static String getCollaboratorSampleId(int kitTypeId, String participantCollaboratorId) {
        SimpleResult results = inTransaction((conn) -> {
            SimpleResult dbVals = new SimpleResult();
            String collaboratorSampleId = KitRequestShipping.generateBspSampleID(conn, participantCollaboratorId, DUMMY_KIT_TYPE_NAME, kitTypeId);
            dbVals.resultValue = collaboratorSampleId;
            return dbVals;
        });
        if (results.resultException != null) {
            throw new RuntimeException("Error getting Collaborator Sample Id for  " + participantCollaboratorId, results.resultException);
        }
        else {
            return (String) results.resultValue;
        }
    }
}
