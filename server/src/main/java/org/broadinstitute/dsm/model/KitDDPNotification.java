package org.broadinstitute.dsm.model;

import lombok.Data;
import lombok.NonNull;
import org.broadinstitute.ddp.db.SimpleResult;
import org.broadinstitute.ddp.db.TransactionWrapper;
import org.broadinstitute.dsm.statics.ApplicationConfigConstants;
import org.broadinstitute.dsm.statics.DBConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.broadinstitute.ddp.db.TransactionWrapper.inTransaction;

@Data
public class KitDDPNotification {

    private static final Logger logger = LoggerFactory.getLogger(KitDDPNotification.class);

    public static final String RECEIVED = "RECEIVED";
    public static final String REMINDER = "REMINDER";
    public static final String SENT = "SENT";

    private final String participantId;
    private final String dsmKitRequestId;
    private final String ddpInstanceId;
    private final String instanceName;
    private final String baseUrl;
    private final String eventName;
    private final String eventType;
    private final long date;
    private final boolean hasAuth0Token;

    public KitDDPNotification(String participantId, String dsmKitRequestId, String ddpInstanceId, String instanceName,
                              String baseUrl, String eventName, String eventType, long date, boolean hasAuth0Token) {
        this.participantId = participantId;
        this.dsmKitRequestId = dsmKitRequestId;
        this.ddpInstanceId = ddpInstanceId;
        this.instanceName = instanceName;
        this.baseUrl = baseUrl;
        this.eventName = eventName;
        this.eventType = eventType;
        this.date = date;
        this.hasAuth0Token = hasAuth0Token;
    }

    public static KitDDPNotification getKitDDPNotification(@NonNull String query, @NonNull String kitLabel) {
        SimpleResult results = inTransaction((conn) -> {
            SimpleResult dbVals = new SimpleResult();
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, kitLabel);
                try (ResultSet rs = stmt.executeQuery()) {
                    rs.last();
                    int count = rs.getRow();
                    rs.beforeFirst();
                    if (count == 1 && rs.next()) { //if row is 0 the ddp/kit type combination does not trigger a participant event
                        dbVals.resultValue = new KitDDPNotification(rs.getString(DBConstants.DDP_PARTICIPANT_ID),
                                rs.getString(DBConstants.DSM_KIT_REQUEST_ID), rs.getString(DBConstants.DDP_INSTANCE_ID),
                                rs.getString(DBConstants.INSTANCE_NAME),
                                rs.getString(DBConstants.BASE_URL),
                                rs.getString(DBConstants.EVENT_NAME),
                                rs.getString(DBConstants.EVENT_TYPE), rs.getLong(DBConstants.DSM_RECEIVE_DATE),
                                rs.getBoolean(DBConstants.NEEDS_AUTH0_TOKEN));
                    }
                }
            }
            catch (Exception ex) {
                dbVals.resultException = ex;
            }
            return dbVals;
        });

        if (results.resultException != null) {
            logger.error("Failed to get KitDDPNotification w/ label " + kitLabel, results.resultException);
        }
        return (KitDDPNotification) results.resultValue;
    }
}
