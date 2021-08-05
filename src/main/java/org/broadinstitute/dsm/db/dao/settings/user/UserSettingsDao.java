package org.broadinstitute.dsm.db.dao.settings.user;

import static org.broadinstitute.ddp.db.TransactionWrapper.inTransaction;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

import org.broadinstitute.ddp.db.SimpleResult;
import org.broadinstitute.dsm.db.dto.settings.UserSettingsDto;

public class UserSettingsDao implements UserSettings {

    private static final String SQL_ROWS_ON_PAGE_BY_USER_ID = "SELECT " +
            "rows_on_page " +
            "FROM user_settings WHERE user_id = ?";
    public static final String ROWS_ON_PAGE = "rows_on_page";

    @Override
    public int create(UserSettingsDto userSettingsDto) {
        return 0;
    }

    @Override
    public int delete(int id) {
        return 0;
    }

    @Override
    public Optional<UserSettingsDto> get(long id) {
        return Optional.empty();
    }

    @Override
    public int getRowsOnPageById(int userId) {
        SimpleResult results = inTransaction((conn) -> {
            SimpleResult execResult = new SimpleResult(-1);
            try (PreparedStatement stmt = conn.prepareStatement(SQL_ROWS_ON_PAGE_BY_USER_ID)) {
                stmt.setInt(1, userId);
                try(ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        execResult.resultValue = rs.getInt(ROWS_ON_PAGE);
                    }
                }
            }
            catch (SQLException ex) {
                execResult.resultException = ex;
            }
            return execResult;
        });
        if (results.resultException != null) {
            throw new RuntimeException("Error getting rows on page for user with id: "
                    + userId, results.resultException);
        }
        return (int) results.resultValue;
    }
}
