package org.broadinstitute.dsm.db.dao.queue;

import com.sun.istack.NotNull;
import lombok.NonNull;
import org.broadinstitute.dsm.db.dao.Dao;
import org.broadinstitute.dsm.db.dto.queue.EventDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Optional;

public class EventDao implements Dao<EventDto> {

        private static final Logger logger = LoggerFactory.getLogger(EventDao.class);

        private static String GET_TRIGGERED_EVENT_QUEUE_BY_EVENT_TYPE_AND_DDP_PARTICIPANT_ID = "SELECT " +
                "EVENT_ID, EVENT_DATE_CREATED, EVENT_TYPE, DDP_INSTANCE_ID, DSM_KIT_REQUEST_ID, DDP_PARTICIPANT_ID, EVENT_TRIGGERED " +
                "FROM EVENT_QUEUE " +
                "WHERE EVENT_TYPE = ? AND DDP_PARTICIPANT_ID = ? AND EVENT_TRIGGERED = 1";

    public static final String EVENT_TYPE = "EVENT_TYPE";

    @Override
    public int create(EventDto eventDto) {
        return 0;
    }

    @Override
    public int delete(int id) {
        return 0;
    }

    @Override
    public Optional<EventDto> get(long id) {
        return Optional.empty();
    }

    public static Boolean hasTriggeredEventByEventTypeAndDdpParticipantId(Connection conn, @NotNull String eventType, @NonNull String ddpParticipantId) {
        try (PreparedStatement stmt = conn.prepareStatement(GET_TRIGGERED_EVENT_QUEUE_BY_EVENT_TYPE_AND_DDP_PARTICIPANT_ID, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY)) {
            stmt.setString(1, eventType);
            stmt.setString(2, ddpParticipantId);
            try (ResultSet rs = stmt.executeQuery()) {
                rs.last();
                int count = rs.getRow();
                if (count > 0) {
                    return true;
                }
            }
        }
        catch (Exception ex) {
            logger.error("Couldn't get triggered event for participant " + ddpParticipantId, ex);
        }
        return false;
    }
}
