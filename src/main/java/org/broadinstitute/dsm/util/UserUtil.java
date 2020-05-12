package org.broadinstitute.dsm.util;

import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.ddp.db.SimpleResult;
import org.broadinstitute.ddp.db.TransactionWrapper;
import org.broadinstitute.dsm.db.UserSettings;
import org.broadinstitute.dsm.statics.ApplicationConfigConstants;
import org.broadinstitute.dsm.statics.DBConstants;
import org.broadinstitute.dsm.statics.QueryExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.QueryParamsMap;
import spark.Request;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import static org.broadinstitute.ddp.db.TransactionWrapper.inTransaction;

public class UserUtil {

    private static final Logger logger = LoggerFactory.getLogger(UserUtil.class);

    private static final String SQL_SELECT_USER = "SELECT user_id, name FROM access_user";
    private static final String SQL_INSERT_USER = "INSERT INTO access_user (name, email) VALUES (?,?)";
    private static final String SQL_SELECT_USER_ACCESS_ROLE = "SELECT role.name FROM access_user_role_group roleGroup, access_user user, access_role role " +
            "WHERE roleGroup.user_id = user.user_id AND roleGroup.role_id = role.role_id AND user.is_active = 1 AND user.email = ?";
    public static final String SQL_USER_ROLES_PER_REALM = "SELECT role.name FROM  access_user_role_group roleGroup " +
            "LEFT JOIN ddp_instance_group gr on (gr.ddp_group_id = roleGroup.group_id) " +
            "LEFT JOIN access_user user on (roleGroup.user_id = user.user_id) " +
            "LEFT JOIN ddp_instance realm on (realm.ddp_instance_id = gr.ddp_instance_id) " +
            "LEFT JOIN access_role role on (role.role_id = roleGroup.role_id) " +
            "WHERE roleGroup.user_id = ? and instance_name = ?";
    private static final String SQL_USER_ROLES = "SELECT DISTINCT role.name FROM  access_user_role_group roleGroup " +
            "LEFT JOIN ddp_instance_group gr on (gr.ddp_group_id = roleGroup.group_id) " +
            "LEFT JOIN access_user user on (roleGroup.user_id = user.user_id) " +
            "LEFT JOIN access_role role on (role.role_id = roleGroup.role_id) " +
            "WHERE roleGroup.user_id = ? ";
    private static final String SQL_SELECT_USER_REALMS = "SELECT DISTINCT realm.instance_name, (SELECT count(role.name) " +
            "FROM ddp_instance realm2, ddp_instance_role inRol, instance_role role " +
            "WHERE realm2.ddp_instance_id = inRol.ddp_instance_id AND inRol.instance_role_id = role.instance_role_id AND role.name = ? " +
            "AND realm2.ddp_instance_id = realm.ddp_instance_id) AS 'has_role' FROM access_user_role_group roleGroup, " +
            "access_user user, ddp_group, ddp_instance_group realmGroup, ddp_instance realm, access_role role " +
            "WHERE realm.ddp_instance_id = realmGroup.ddp_instance_id AND realmGroup.ddp_group_id = ddp_group.group_id AND ddp_group.group_id = roleGroup.group_id " +
            "AND roleGroup.user_id = user.user_id AND role.role_id = roleGroup.role_id AND realm.is_active = 1 AND user.is_active = 1 AND user.user_id = ? ";

    public static final String USER_ID = "userId";

    private static final String NO_USER_ROLE = "NO_USER_ROLE";

    //TODO sql is wrong user will not be found anymore with new auth0 roles
    public ArrayList<String> getUserAccessRoles(@NonNull String email) {
        ArrayList<String> roles = new ArrayList<>();
        SimpleResult results = inTransaction((conn) -> {
            SimpleResult dbVals = new SimpleResult();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_SELECT_USER_ACCESS_ROLE)) {
                stmt.setString(1, email);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        roles.add(rs.getString(DBConstants.NAME));
                    }
                }
            }
            catch (SQLException ex) {
                dbVals.resultException = ex;
            }
            return dbVals;
        });

        if (results.resultException != null) {
            throw new RuntimeException("Error getting list of roles ", results.resultException);
        }
        return roles;
    }

    public static Map<Integer, String> getUserMap() {
        Map<Integer, String> users = new HashMap<>();
        SimpleResult results = inTransaction((conn) -> {
            SimpleResult dbVals = new SimpleResult();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_SELECT_USER)) {
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        users.put(rs.getInt(DBConstants.USER_ID), rs.getString(DBConstants.NAME));
                    }
                }
            }
            catch (SQLException ex) {
                dbVals.resultException = ex;
            }
            return dbVals;
        });

        if (results.resultException != null) {
            throw new RuntimeException("Error getting map of users ", results.resultException);
        }
        return users;
    }

    public static String getUserId(Request request) {
        QueryParamsMap queryParams = request.queryMap();
        String userId = "";
        if (queryParams.value(USER_ID) != null) {
            userId = queryParams.get(USER_ID).value();
        }

        if (StringUtils.isBlank(userId)) {
            throw new RuntimeException("No userId query param was sent");
        }
        return userId;
    }

    public static Collection<String> getListOfAllowedRealms(@NonNull String userId) {
        List<String> listOfRealms = new ArrayList<>();
        SimpleResult results = inTransaction((conn) -> {
            SimpleResult dbVals = new SimpleResult();
            try {
                getList(conn, SQL_SELECT_USER_REALMS, NO_USER_ROLE, userId, listOfRealms);
            }
            catch (SQLException ex) {
                dbVals.resultException = ex;
            }
            return dbVals;
        });

        if (results.resultException != null) {
            throw new RuntimeException("Couldn't get lists of allowed realms ", results.resultException);
        }
        logger.info("Found " + listOfRealms.size() + " realm for user w/ id " + userId);
        return listOfRealms;
    }

    private static void getList(Connection conn, String query, String userId, List<String> listOfRealms) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    if (rs.getBoolean(DBConstants.HAS_ROLE)) {
                        listOfRealms.add(rs.getString(DBConstants.INSTANCE_NAME));
                    }
                }
            }
        }
    }

    private static void getList(Connection conn, String query, String role, String userId, List<String> listOfRealms) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, role);
            stmt.setString(2, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    if (NO_USER_ROLE.equals(role)) {
                        listOfRealms.add(rs.getString(DBConstants.INSTANCE_NAME));
                    }
                    else {
                        if (rs.getBoolean(DBConstants.HAS_ROLE)) {
                            listOfRealms.add(rs.getString(DBConstants.INSTANCE_NAME));
                        }
                    }
                }
            }
        }
    }

    public static boolean checkUserAccess(String realm, String userId, String role) {
        List<String> roles;
        if (StringUtils.isBlank(realm)) {
            roles = getUserRolesPerRealm(SQL_USER_ROLES, userId, null);
        }
        else {
            roles = getUserRolesPerRealm(SQL_USER_ROLES_PER_REALM, userId, realm);
        }
        if (roles != null && !roles.isEmpty()) {
            return roles.contains(role);
        }
        return false;
    }

    public static List<String> getUserRolesPerRealm(@NonNull String query, @NonNull String userId, String realm) {
        List<String> roles = new ArrayList<>();
        SimpleResult results = inTransaction((conn) -> {
            SimpleResult dbVals = new SimpleResult();
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, userId);
                if (StringUtils.isNotBlank(realm)) {
                    stmt.setString(2, realm);
                }
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        roles.add(rs.getString(DBConstants.NAME));
                    }
                }
            }
            catch (SQLException ex) {
                dbVals.resultException = ex;
            }
            return dbVals;
        });

        if (results.resultException != null) {
            throw new RuntimeException("Error getting list of user roles ", results.resultException);
        }
        return roles;
    }
}
