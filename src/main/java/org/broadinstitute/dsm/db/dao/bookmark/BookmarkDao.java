package org.broadinstitute.dsm.db.dao.bookmark;

import static org.broadinstitute.ddp.db.TransactionWrapper.inTransaction;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

import org.broadinstitute.ddp.db.SimpleResult;
import org.broadinstitute.dsm.db.dao.Dao;
import org.broadinstitute.dsm.db.dto.bookmark.BookmarkDto;
import org.broadinstitute.dsm.db.dto.participant.data.ParticipantDataDto;
import org.broadinstitute.dsm.statics.DBConstants;

public class BookmarkDao implements Dao<BookmarkDto> {

    private static final String SQL_GET_BOOKMARK_BY_INSTANCE_NAME = "SELECT " +
            "bookmark_id, " +
            "value, " +
            "instance " +
            "FROM bookmark WHERE instance = ?";

    private static final String SQL_UPDATE_BOOKMARK = "UPDATE " +
            "bookmark SET value = ?, instance = ? WHERE bookmark_id = ?";

    private static final String SQL_UPDATE_BOOKMARK_VALUE_BY_BOOKMARK_ID = "UPDATE " +
            "bookmark SET value = ? WHERE bookmark_id = ?";

    @Override
    public int create(BookmarkDto bookmarkDto) {
        return 0;
    }

    @Override
    public int delete(int id) {
        return 0;
    }

    @Override
    public Optional<BookmarkDto> get(long id) {
        return Optional.empty();
    }

    public Optional<BookmarkDto> getBookmarkByInstance(String instanceName) {
        SimpleResult results = inTransaction((conn) -> {
            SimpleResult dbVals = new SimpleResult();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_GET_BOOKMARK_BY_INSTANCE_NAME)) {
                stmt.setString(1, instanceName);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        dbVals.resultValue = new BookmarkDto.Builder(rs.getLong(2),rs.getString(3))
                                .withBookmarkId(rs.getInt(1))
                                .build();
                    }
                }
                catch (SQLException e) {
                    throw new RuntimeException("Error getting bookmark with instance: " + instanceName, e);
                }
            }
            catch (SQLException ex) {
                dbVals.resultException = ex;
            }
            return dbVals;
        });

        if (results.resultException != null) {
            throw new RuntimeException("Couldn't get bookmark with instance name: " + instanceName, results.resultException);
        }
        return Optional.ofNullable((BookmarkDto) results.resultValue);
    }

    public int updateBookmark(BookmarkDto bookmarkDto) {
        SimpleResult result = inTransaction((conn) -> {
            SimpleResult execResult = new SimpleResult();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_UPDATE_BOOKMARK)) {
                stmt.setLong(1, bookmarkDto.getValue());
                stmt.setString(2, bookmarkDto.getInstance());
                stmt.setInt(3, bookmarkDto.getBookmarkId());
                execResult.resultValue = stmt.executeUpdate();
            } catch (SQLException sqle) {
                execResult.resultException = sqle;
            }
            return execResult;
        });
        if (result.resultException != null) {
            throw new RuntimeException("Could not update bookmark with id: " + bookmarkDto.getBookmarkId());
        }
        return (int) result.resultValue;
    }

    public int updateBookmarkValueByBookmarkId(int bookmarkId, long value) {
        SimpleResult result = inTransaction((conn) -> {
            SimpleResult execResult = new SimpleResult();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_UPDATE_BOOKMARK_VALUE_BY_BOOKMARK_ID)) {
                stmt.setLong(1, value);
                stmt.setInt(2, bookmarkId);
                execResult.resultValue = stmt.executeUpdate();
            } catch (SQLException sqle) {
                execResult.resultException = sqle;
            }
            return execResult;
        });
        if (result.resultException != null) {
            throw new RuntimeException("Could not update bookmark value with id: " + bookmarkId);
        }
        return (int) result.resultValue;
    }


}
