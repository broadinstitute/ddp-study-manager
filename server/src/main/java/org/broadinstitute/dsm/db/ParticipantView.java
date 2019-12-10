package org.broadinstitute.dsm.db;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Data;
import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.ddp.db.SimpleResult;
import org.broadinstitute.dsm.model.ParticipantColumn;
import org.broadinstitute.dsm.statics.DBConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

import static org.broadinstitute.ddp.db.TransactionWrapper.inTransaction;

@Data
public class ParticipantView {

    private static final Logger logger = LoggerFactory.getLogger(ParticipantView.class);

    private static final String SQL_INSERT_VIEW = "INSERT INTO mr_tissue_views SET user_id = ?, name = ?, view_json = ?, shared = ?";
    private static final String SQL_UPDATE_VIEW = "UPDATE mr_tissue_views SET name = ?, view_json = ?, shared = ? WHERE mr_tissue_views_id = ?";
    private static final String SQL_DELETE_VIEW = "UPDATE mr_tissue_views SET deleted = 1 WHERE mr_tissue_views_id = ?";
    private static final String SQL_SELECT_VIEW = "SELECT DISTINCT mrV.mr_tissue_views_id, mrV.name, view_json, shared, mrV.user_id, user.name" +
            " FROM mr_tissue_views mrV, access_user user WHERE user.user_id = mrV.user_id AND mrV.user_id IN (SELECT DISTINCT user_id FROM access_user_role_group urg," +
            " access_role r WHERE r.role_id = urg.role_id AND r.name = \"mr_view\" AND urg.group_id in (SELECT group_id FROM access_user_role_group urg, access_role r" +
            " WHERE user_id = ? AND r.role_id = urg.role_id AND r.name = \"mr_view\" )) AND ((mrV.user_id != ? AND mrV.shared = 1) OR mrV.user_id = ?)";
    private static final String SQL_SELECT_FAV_VIEW = "SELECT user_settings_id, user_id FROM user_settings WHERE json_contains(`fav_views`, '{\"id\" : %1}')";
    private static final String SQL_SELECT_USER_FAV_VIEW = "SELECT fav_views FROM user_settings WHERE user_id = ?";
    private static final String SQL_UPDATE_USER_FAV_VIEW = "UPDATE user_settings SET fav_views = ? WHERE user_id = ?";

    private int id;
    private boolean fav;
    private Boolean shared;
    private String userId;
    private String user;
    private String summary;
    private List<ParticipantColumn> customColumns;

    public ParticipantView(int id, boolean fav) {
        this.id = id;
        this.fav = fav;
    }

    public ParticipantView(int id, boolean shared, String userId, String user, String summary, List<ParticipantColumn> customColumns) {
        this.id = id;
        this.shared = shared;
        this.userId = userId;
        this.user = user;
        this.summary = summary;
        this.customColumns = customColumns;
    }

    public static Integer saveView(@NonNull String userId, @NonNull String summary, boolean shared, @NonNull List<ParticipantColumn> customColumns) {
        String columnJson = new GsonBuilder().create().toJson(customColumns);
        if (StringUtils.isNotBlank(columnJson)) {
            SimpleResult results = inTransaction((conn) -> {
                SimpleResult dbVals = new SimpleResult();
                try (PreparedStatement stmt = conn.prepareStatement(SQL_INSERT_VIEW, Statement.RETURN_GENERATED_KEYS)) {
                    stmt.setString(1, userId);
                    stmt.setString(2, summary);
                    stmt.setString(3, columnJson);
                    stmt.setBoolean(4, shared);
                    int result = stmt.executeUpdate();
                    if (result == 1) {
                        try (ResultSet rs = stmt.getGeneratedKeys()) {
                            if (rs.next()) {
                                int viewId = rs.getInt(1);
                                logger.info("Added new view " + viewId);
                                dbVals.resultValue = viewId;
                            }
                        }
                        catch (Exception e) {
                            throw new RuntimeException("Error getting id of new institution ", e);
                        }
                    }
                    else {
                        throw new RuntimeException("Error adding new view " + result + " rows");
                    }
                }
                catch (SQLException ex) {
                    dbVals.resultException = ex;
                }
                return dbVals;
            });

            if (results.resultException != null) {
                throw new RuntimeException("Error adding new view", results.resultException);
            }
            else {
                return (int) results.resultValue;
            }
        }
        return null;
    }

    public static void changeView(@NonNull int id, @NonNull String summary, @NonNull List<ParticipantColumn> customColumns, boolean share) {
        String columnJson = new GsonBuilder().create().toJson(customColumns);
        if (StringUtils.isNotBlank(columnJson)) {
            SimpleResult results = inTransaction((conn) -> {
                SimpleResult dbVals = new SimpleResult();
                try (PreparedStatement stmt = conn.prepareStatement(SQL_UPDATE_VIEW)) {
                    stmt.setString(1, summary);
                    stmt.setString(2, columnJson);
                    stmt.setBoolean(3, share);
                    stmt.setInt(4, id);
                    int result = stmt.executeUpdate();
                    if (result == 1) {
                        logger.info("Changed view ");
                    }
                    else {
                        throw new RuntimeException("Error changing view " + result + " rows");
                    }
                }
                catch (SQLException ex) {
                    dbVals.resultException = ex;
                }
                return dbVals;
            });

            if (results.resultException != null) {
                throw new RuntimeException("Error changing view", results.resultException);
            }
        }
    }

    public static void changeFavoriteSetting(@NonNull String userId, @NonNull int id, boolean fav) {
        List<ParticipantView> favViews = getFavViews(userId);
        if (fav) {
            favViews.add(new ParticipantView(id, fav));
        }
        else {
            for (Iterator<ParticipantView> iter = favViews.iterator(); iter.hasNext();) {
                ParticipantView view = iter.next();
                if (view.getId() == id) {
                    iter.remove();
                }
            }
        }
        updateFavViews(userId, favViews);
    }

    public static List<ParticipantView> getUserFavViews(@NonNull String userId) {
        List<ParticipantView> views = getViews(userId);
        List<ParticipantView> favs = getFavViews(userId);
        for (ParticipantView fav : favs) {
            for (ParticipantView view : views) {
                if (view.getId() == fav.getId()) {
                    view.setFav(true);
                    break;
                }
            }
        }
        return views;
    }

    public static List<ParticipantView> getViews(@NonNull String userId) {
        List<ParticipantView> views = new ArrayList<>();
        SimpleResult results = inTransaction((conn) -> {
            SimpleResult dbVals = new SimpleResult();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_SELECT_VIEW)) {
                stmt.setString(1, userId);
                stmt.setString(2, userId);
                stmt.setString(3, userId);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        List<ParticipantColumn> columns = Arrays.asList(new Gson().fromJson(rs.getString(DBConstants.VIEW_JSON), ParticipantColumn[].class));
                        views.add(new ParticipantView(rs.getInt(DBConstants.MR_TISSUE_VIEWS_ID),
                                rs.getBoolean(DBConstants.SHARED), rs.getString(DBConstants.USER_ID),
                                rs.getString("user." + DBConstants.NAME),
                                rs.getString("mrV." + DBConstants.NAME),
                                columns));
                    }
                }
            }
            catch (SQLException ex) {
                dbVals.resultException = ex;
            }
            return dbVals;
        });

        if (results.resultException != null) {
            throw new RuntimeException("Error getting list of view", results.resultException);
        }
        return views;
    }

    public static List<ParticipantView> getFavViews(@NonNull String userId) {
        SimpleResult results = inTransaction((conn) -> {
            SimpleResult dbVals = new SimpleResult();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_SELECT_USER_FAV_VIEW)) {
                stmt.setString(1, userId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        String fav = rs.getString(DBConstants.FAVORITE_VIEWS);
                        if (StringUtils.isNotBlank(fav)) {
                            dbVals.resultValue = new LinkedList<>(Arrays.asList(new Gson().fromJson(fav, ParticipantView[].class)));
                        }
                        else {
                            dbVals.resultValue = new LinkedList<>();
                        }
                    }
                }
            }
            catch (SQLException ex) {
                dbVals.resultException = ex;
            }
            return dbVals;
        });

        if (results.resultException != null) {
            throw new RuntimeException("Error getting list of view", results.resultException);
        }
        return (List<ParticipantView>) results.resultValue;
    }

    public static void updateFavViews(@NonNull String userId, @NonNull List<ParticipantView> favViews) {
        String columnJson = new GsonBuilder().create().toJson(favViews);
        if (StringUtils.isNotBlank(columnJson)) {
            SimpleResult results = inTransaction((conn) -> {
                SimpleResult dbVals = new SimpleResult();
                try (PreparedStatement stmt = conn.prepareStatement(SQL_UPDATE_USER_FAV_VIEW)) {
                    stmt.setString(1, columnJson);
                    stmt.setString(2, userId);
                    int result = stmt.executeUpdate();
                    if (result == 1) {
                        logger.info("Changed fav views ");
                    }
                    else {
                        throw new RuntimeException("Error changing fav views " + result + " rows");
                    }
                }
                catch (SQLException ex) {
                    dbVals.resultException = ex;
                }
                return dbVals;
            });

            if (results.resultException != null) {
                logger.info(columnJson);
                throw new RuntimeException("Error changing fav views", results.resultException);
            }
        }
    }

    public static void deleteView(@NonNull int id) {
        SimpleResult results = inTransaction((conn) -> {
            SimpleResult dbVals = new SimpleResult();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_DELETE_VIEW)) {
                stmt.setInt(1, id);
                int result = stmt.executeUpdate();
                if (result == 1) {
                    logger.info("Deleted view ");
                } else {
                    throw new RuntimeException("Error deleting view w/ id " + id + ". Query changed " + result + " rows");
                }
            } catch (SQLException ex) {
                dbVals.resultException = ex;
            }
            return dbVals;
        });

        if (results.resultException != null) {
            throw new RuntimeException("Error deleting view", results.resultException);
        }
    }

    public static List<String> getUserIds(@NonNull int id) {
        List<String> userIds = new ArrayList<>();
        SimpleResult results = inTransaction((conn) -> {
            SimpleResult dbVals = new SimpleResult();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_SELECT_FAV_VIEW.replace("%1", String.valueOf(id)))) {
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        userIds.add(rs.getString(DBConstants.USER_ID));
                    }
                }
            }
            catch (SQLException ex) {
                dbVals.resultException = ex;
            }
            return dbVals;
        });

        if (results.resultException != null) {
            throw new RuntimeException("Error getting list of userIDs", results.resultException);
        }
        return userIds;
    }
}
