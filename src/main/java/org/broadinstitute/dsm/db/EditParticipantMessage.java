package org.broadinstitute.dsm.db;

import lombok.NonNull;
import org.broadinstitute.ddp.db.SimpleResult;
import org.broadinstitute.dsm.statics.DBConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.broadinstitute.ddp.db.TransactionWrapper.inTransaction;

public class EditParticipantMessage {

    private static final Logger logger = LoggerFactory.getLogger(EditParticipantMessage.class);

    private static final String SQL_SELECT_MESSAGE_AND_STATUS =
            "SELECT " +
                "message_status, received_message " +
            "FROM " +
                "message " +
            "WHERE " +
                "user_id = ? " +
            "ORDER BY published_at DESC " +
            "LIMIT 1";

    private static final String SQL_INSERT_MESSAGE =
            "INSERT INTO " +
                    "message " +
                    "(user_id, message_status, published_at) " +
            "VALUES " +
                    "(?, ?, ?)";

    private static final String SQL_UPDATE_MESSAGE =
            "UPDATE " +
                    "message " +
            "SET " +
                    "message_status = ?, received_message = ?, received_at = ? " +
            "WHERE " +
                    " user_id = ?";

    private int messageId;
    private int userId;
    private String messageStatus;
    private long published_at;

    public EditParticipantMessage(int userId, String messageStatus, long published_at) {
        this.userId = userId;
        this.messageStatus = messageStatus;
        this.published_at = published_at;
    }

    public long getPublished_at() {
        return published_at;
    }

    public void setPublished_at(long published_at) {
        this.published_at = published_at;
    }

    public int getMessageId() {
        return messageId;
    }

    public void setMessageId(int messageId) {
        this.messageId = messageId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getMessageWithStatus() {
        return messageStatus;
    }

    public void setMessageStatus(String messageStatus) {
        this.messageStatus = messageStatus;
    }

    public static List<String> getMessageWithStatus(String userId) {
        List<String> messagesWithStatus = new ArrayList<>();
        SimpleResult results = inTransaction((conn) -> {
            SimpleResult dbVals = new SimpleResult();

            try (PreparedStatement stmt = conn.prepareStatement(SQL_SELECT_MESSAGE_AND_STATUS)) {
                stmt.setString(1, userId);
                try (ResultSet rs = stmt.executeQuery()) {
                    messagesWithStatus.add(rs.getString(DBConstants.MESSAGE_STATUS));
                    messagesWithStatus.add(rs.getString(DBConstants.RECEIVED_MESSAGE));
                }
            }
            catch (SQLException ex) {
                dbVals.resultException = ex;
            }
            return dbVals;
        });

        if (results.resultException != null) {
            throw new RuntimeException("Error getting message status ", results.resultException);
        }

        return messagesWithStatus;
    }

    public static void insertMessage(@NonNull EditParticipantMessage message) {
        SimpleResult results = inTransaction((conn) -> {
            SimpleResult dbVals = new SimpleResult();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_INSERT_MESSAGE)) {
                stmt.setInt(1, message.getUserId());
                stmt.setString(2, message.getMessageWithStatus());
                stmt.setLong(3, message.getPublished_at());
                int result = stmt.executeUpdate();
                if (result == 1) {
                    logger.info("Added new message ");
                }
                else {
                    throw new RuntimeException("Error adding new message, it was updating " + result + " rows");
                }
            }
            catch (SQLException ex) {
                dbVals.resultException = ex;
            }
            return dbVals;
        });
        if (results.resultException != null) {
            throw new RuntimeException("Error inserting message for the user with user ID: "
                    + message.getUserId(), results.resultException);
        }
    }

    public static void updateMessage(@NonNull String userId, @NonNull String messageStatus, @NonNull String message, @NonNull long received_at) {
        SimpleResult results = inTransaction((conn) -> {
            SimpleResult dbVals = new SimpleResult();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_UPDATE_MESSAGE)) {
                stmt.setString(1, messageStatus);
                stmt.setString(2, message);
                stmt.setLong(3, received_at);
                int result = stmt.executeUpdate();
                if (result == 1) {
                    logger.info("Updating message status of user with id: " + userId);
                }
                else {
                    throw new RuntimeException("Error updating message status of user with " + userId + ". it was updating " + result + " rows");
                }
            }
            catch (SQLException ex) {
                dbVals.resultException = ex;
            }
            return dbVals;
        });
        if (results.resultException != null) {
            throw new RuntimeException("Error updating message status of user with: " + userId, results.resultException);
        }
    }

}
