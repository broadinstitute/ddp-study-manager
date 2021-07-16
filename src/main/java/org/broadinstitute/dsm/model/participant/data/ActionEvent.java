package org.broadinstitute.dsm.model.participant.data;

import com.sun.istack.NotNull;
import lombok.Data;
import lombok.NonNull;
import org.broadinstitute.ddp.db.SimpleResult;
import org.broadinstitute.dsm.statics.DBConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;

@Data
public class ActionEvent {

    private static final Logger logger = LoggerFactory.getLogger(ActionEvent.class);

    public static final String EVENT = "PARTICIPANT_EVENT";
    public static final String RECEIVED = "RECEIVED";
    public static final String SENT = "SENT";

    private static String SELECT_ACTION_EVENT = "SELECT " +
            "eve.event_name, eve.event_type, " +
            "realm.ddp_instance_id, realm.instance_name, realm.base_url, realm.auth0_token " +
            "FROM " +
            "event_type eve, " +
            "ddp_instance realm " +
            "WHERE " +
            "eve.ddp_instance_id = realm.ddp_instance_id " +
            "AND eve.event_name = ? " +
            "AND realm.ddp_instance_id = ?";

    private final String ddpInstanceId;
    private final String instanceName;
    private final String baseUrl;
    private final String eventName;
    private final String eventType;
    private final boolean hasAuth0Token;

    public ActionEvent(String ddpInstanceId, String instanceName, String baseUrl, String eventName,
                       String eventType, boolean hasAuth0Token) {
        this.ddpInstanceId = ddpInstanceId;
        this.instanceName = instanceName;
        this.baseUrl = baseUrl;
        this.eventName = eventName;
        this.eventType = eventType;
        this.hasAuth0Token = hasAuth0Token;
    }

    public static ActionEvent getParticipantEvent(Connection conn, @NotNull String eventType, @NonNull String instanceId) {
        ArrayList<String> skippedEvents = new ArrayList();
        SimpleResult dbVals = new SimpleResult();
        try (PreparedStatement stmt = conn.prepareStatement(SELECT_ACTION_EVENT, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY)) {
            stmt.setString(1, eventType);
            stmt.setString(2, instanceId);
            try (ResultSet rs = stmt.executeQuery()) {
                rs.last();
                int count = rs.getRow();
                rs.beforeFirst();
                if (count == 1) {
                    if (rs.next()) { //if row is 0 the ddp/kit type combination does not trigger a participant event
                        dbVals.resultValue = new ActionEvent(
                                rs.getString(DBConstants.DDP_INSTANCE_ID),
                                rs.getString(DBConstants.INSTANCE_NAME),
                                rs.getString(DBConstants.BASE_URL),
                                rs.getString(DBConstants.EVENT_NAME),
                                rs.getString(DBConstants.EVENT_TYPE),
                                rs.getBoolean(DBConstants.NEEDS_AUTH0_TOKEN));
                    }
                }
            }
        }
        catch (Exception ex) {
            logger.error("Couldn't get exited participants for " + instanceId, ex);
        }
        return (ActionEvent) dbVals.resultValue;
    }
}
