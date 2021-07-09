package org.broadinstitute.dsm.db;

import lombok.Data;
import lombok.NonNull;
import org.broadinstitute.ddp.db.SimpleResult;
import org.broadinstitute.dsm.statics.DBConstants;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.broadinstitute.ddp.db.TransactionWrapper.inTransaction;

@Data
public class User {

    private static final String SQL_SELECT_USER = "SELECT user.user_id, user.name, user.email, user.phone_number FROM access_user user WHERE user.email = ?";
    private static final String SQL_SELECT_USER_BY_ID = "SELECT user.user_id, user.name, user.email, user.phone_number FROM access_user user WHERE user.user_id = ?";

    private String id;
    private String name;
    private String email;
    private String phoneNumber;

    public User(String id, String name, String email, String phoneNumber) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phoneNumber = phoneNumber;
    }

    public static User getUser(@NonNull String email) {
        SimpleResult results = inTransaction((conn) -> {
            SimpleResult dbVals = new SimpleResult();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_SELECT_USER)) {
                stmt.setString(1, email);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        dbVals.resultValue = new User(rs.getString(DBConstants.USER_ID),
                                rs.getString(DBConstants.NAME),
                                rs.getString(DBConstants.EMAIL),
                                rs.getString(DBConstants.PHONE_NUMBER));
                    }
                }
            }
            catch (SQLException ex) {
                dbVals.resultException = ex;
            }
            return dbVals;
        });

        if (results.resultException != null) {
            throw new RuntimeException("Error getting list of realms ", results.resultException);
        }
        return (User) results.resultValue;
    }

    public static User getUser(@NonNull int userId) {
        SimpleResult results = inTransaction((conn) -> {
            SimpleResult dbVals = new SimpleResult();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_SELECT_USER_BY_ID)) {
                stmt.setInt(1, userId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        dbVals.resultValue = new User(rs.getString(DBConstants.USER_ID),
                                rs.getString(DBConstants.NAME),
                                rs.getString(DBConstants.EMAIL),
                                rs.getString(DBConstants.PHONE_NUMBER));
                    }
                }
            }
            catch (SQLException ex) {
                dbVals.resultException = ex;
            }
            return dbVals;
        });

        if (results.resultException != null) {
            throw new RuntimeException("Error getting list of realms ", results.resultException);
        }
        return (User) results.resultValue;
    }

    public Integer getUserId() {
        return Integer.parseInt(id);
    }
}
