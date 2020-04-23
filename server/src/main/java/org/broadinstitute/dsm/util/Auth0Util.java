package org.broadinstitute.dsm.util;

import com.auth0.client.auth.AuthAPI;
import com.auth0.client.mgmt.ManagementAPI;
import com.auth0.exception.Auth0Exception;
import com.auth0.json.auth.TokenHolder;
import com.auth0.json.mgmt.Permission;
import com.auth0.json.mgmt.PermissionsPage;
import com.auth0.json.mgmt.Role;
import com.auth0.json.mgmt.RolesPage;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.net.AuthRequest;
import com.auth0.net.Request;
import lombok.NonNull;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.dsm.model.auth.AccessRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class Auth0Util {
    private static final Logger logger = LoggerFactory.getLogger(Auth0Util.class);
    private byte[] decodedSecret;
    private final String account;
    private final String mgtApiUrl;
    private AuthAPI ddpAuthApi;
    private AuthAPI mgtAuthApi;
    private ManagementAPI mgmtApi;
    private String audience;
    private String token;
    private Long expiresAt;

    public Auth0Util(@NonNull String account, boolean secretEncoded, @NonNull String ddpKey, @NonNull String ddpSecret, @NonNull String mgtKey,
                     @NonNull String mgtSecret, @NonNull String mgtApiUrl, String audience) {
        this.ddpAuthApi = null;
        this.mgtAuthApi = null;
        byte[] tempSecret = ddpSecret.getBytes();
        if (secretEncoded) {
            tempSecret = Base64.decodeBase64(ddpSecret);
        }
        this.decodedSecret = tempSecret;
        this.account = account;
        this.ddpAuthApi = new AuthAPI(account, ddpKey, ddpSecret);
        this.mgtAuthApi = new AuthAPI(account, mgtKey, mgtSecret);
        this.audience = audience;
        this.mgtApiUrl = mgtApiUrl;
        this.mgmtApi = this.configManagementApi();
    }

    private ManagementAPI configManagementApi() {
        TokenHolder tokenHolder = null;
        try {
            AuthRequest requestToken = this.mgtAuthApi.requestToken(this.mgtApiUrl);
            tokenHolder = requestToken.execute();
            if (tokenHolder.getAccessToken() == null) {
                throw new RuntimeException("Unable to retrieve access token.");
            }
        }
        catch (Exception e) {
            throw new RuntimeException("Unable to generate token using management client.", e);
        }
        String mgmtToken = tokenHolder.getAccessToken();
        return new ManagementAPI(this.account, mgmtToken);
    }

    public Auth0Util.Auth0UserInfo getAuth0UserInfo(@NonNull String idToken) {
        Map<String, Claim> auth0Claims = this.verifyAndParseAuth0TokenClaims(idToken);
        Auth0Util.Auth0UserInfo userInfo = new Auth0Util.Auth0UserInfo((auth0Claims.get("sub")).asString(), (auth0Claims.get("email")).asString(), (auth0Claims.get("exp")).asInt());
        return userInfo;
    }

    public List<String> getUserPermissions(@NonNull String userId, @NonNull String email, String realm) {
        if (StringUtils.isNotBlank(realm)) {
            return getUserRoles(userId, email, realm);
        }
        return getUserPermissions(userId, email);
    }

    public boolean hasUserPermission(@NonNull String userId, @NonNull String email, String realm, String neededPermission) {
        List<String> permissions = null;
        if (StringUtils.isNotBlank(realm)) {
            //does user have special permissions for the selected realm
            permissions = getUserRoles(userId, email, realm);
        }
        if (permissions == null || permissions.isEmpty()) {
            //if user doesn't have any special permissions for the selected realm get general permissions
            permissions = getUserPermissions(userId, email);
        }
        if (permissions != null && !permissions.isEmpty()) {
            List<String> filteredPermissions = permissions.stream()
                    .filter(permission -> permission.equals(neededPermission))
                    .collect(Collectors.toList());
            if (filteredPermissions != null && !filteredPermissions.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public boolean hasUserPermission(@NonNull List<String> permissions, String neededPermission) {
        if (permissions != null && !permissions.isEmpty()) {
            List<String> filteredPermissions = permissions.stream()
                    .filter(permission -> permission.equals(neededPermission))
                    .collect(Collectors.toList());
            if (filteredPermissions != null && !filteredPermissions.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public List<String> getUserPermissions(@NonNull String userId, @NonNull String email) {
        logger.info("Getting user permissions");
        try {
            if (this.mgmtApi == null) {
                this.mgmtApi = this.configManagementApi();
            }
            Request<PermissionsPage> permissionsPageRequest = this.mgmtApi.users().listPermissions(userId, null);
            PermissionsPage permissionsPage = permissionsPageRequest.execute();
            List<Permission> permissionsList = permissionsPage.getItems();
            List<String> permissions = permissionsList.stream()
                    .map(object -> Objects.toString(object.getName(), null))
                    .collect(Collectors.toList());
            logger.info("Returning user permissions");
            return permissions;
        }
        catch (Exception e) {
            throw new RuntimeException("Getting user permissions failed for user " + email, e);
        }
    }

    private List<String> getUserRoles(@NonNull String userId, @NonNull String email, @NonNull String realm) {
        logger.info("Getting user permissions per roles");
        try {
            if (this.mgmtApi == null) {
                this.mgmtApi = this.configManagementApi();
            }
            Request<RolesPage> rolesPageRequest = this.mgmtApi.users().listRoles(userId, null);
            RolesPage rolesPage = rolesPageRequest.execute();
            List<Role> rolesList = rolesPage.getItems();
            List<Role> realmRoles = rolesList.stream()
                    .filter(role -> role.getName().startsWith("instance:" + realm))
                    .collect(Collectors.toList());
            List<Role> generalRoles = rolesList.stream()
                    .filter(role -> !role.getName().startsWith("instance:"))
                    .collect(Collectors.toList());
            if (realmRoles != null && !realmRoles.isEmpty()) {
                return getPermissions(realmRoles);
            }
            else if (generalRoles != null && !generalRoles.isEmpty()) {
                return getPermissions(generalRoles);
            }
        }
        catch (Exception e) {
            throw new RuntimeException("Getting user roles failed for user " + email, e);
        }
        return null;
    }

    public List<AccessRole> getAccess(@NonNull String userId, @NonNull String email) {
        logger.info("Getting user access");
        try {
            if (this.mgmtApi == null) {
                this.mgmtApi = this.configManagementApi();
            }
            Request<RolesPage> rolesPageRequest = this.mgmtApi.users().listRoles(userId, null);
            RolesPage rolesPage = rolesPageRequest.execute();
            List<Role> rolesList = rolesPage.getItems();
            if (rolesList != null && !rolesList.isEmpty()) {
                return getAccessRoles(rolesList);
            }
        }
        catch (Exception e) {
            throw new RuntimeException("Getting user roles failed for user " + email, e);
        }
        return null;
    }

    private List<String> getPermissions(@NonNull List<Role> roles) throws Auth0Exception {
        List<String> permissions = new ArrayList<>();
        for (Role role : roles) {
            Request<PermissionsPage> permissionsPageRequest = this.mgmtApi.roles().listPermissions(role.getId(), null);
            PermissionsPage permissionsPage = permissionsPageRequest.execute();
            List<Permission> permissionsList = permissionsPage.getItems();
            permissions = permissionsList.stream()
                    .map(object -> Objects.toString(object.getName(), null))
                    .collect(Collectors.toList());

        }
        logger.info("Returning user permissions");
        return permissions;
    }

    private List<AccessRole> getAccessRoles(@NonNull List<Role> roles) throws Auth0Exception {
        List<AccessRole> accessRoles = new ArrayList<>();
        for (Role role : roles) {
            Request<PermissionsPage> permissionsPageRequest = this.mgmtApi.roles().listPermissions(role.getId(), null);
            PermissionsPage permissionsPage = permissionsPageRequest.execute();
            List<Permission> permissionsList = permissionsPage.getItems();
            List<String> permissions = permissionsList.stream()
                    .map(object -> Objects.toString(object.getName(), null))
                    .collect(Collectors.toList());
            accessRoles.add(new AccessRole(role.getName(), permissions));
        }
        logger.info("Returning user permissions");
        return accessRoles;
    }

    public Map<String, Claim> verifyAndParseAuth0TokenClaims(String auth0Token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(this.decodedSecret);
            JWTVerifier verifier = JWT.require(algorithm).withIssuer(this.account).build();
            DecodedJWT jwt = verifier.verify(auth0Token);
            Map<String, Claim> auth0Claims = jwt.getClaims();
            return auth0Claims;
        }
        catch (Exception e) {
            throw new RuntimeException("Could not verify auth0 token.", e);
        }
    }

    /**
     * Auth0 token for pepper communication
     *
     * @return
     */
    public String getAccessToken() {
        if (token != null && StringUtils.isNotBlank(token) && expiresAt != null) {
            long minFromNow = System.currentTimeMillis() + (60 * 5);
            if (expiresAt < minFromNow) {
                logger.info("Token will expire in less than 5 min.");
                token = null;
            }
        }
        if (token == null) {
            if (StringUtils.isNotBlank(audience)) {
                try {
                    AuthRequest request = ddpAuthApi.requestToken(audience);
                    TokenHolder tokenHolder = request.execute();
                    token = tokenHolder.getAccessToken();
                    expiresAt = System.currentTimeMillis() + (tokenHolder.getExpiresIn() * 1000);
                    logger.info("Generated new token for auth0.");
                }
                catch (Exception ex) {
                    throw new RuntimeException("Unable to get access token for audience " + audience, ex);
                }
            }
            else {
                throw new RuntimeException("Auth0 Audience is missing.");
            }
        }
        return token;
    }

    public static class Auth0UserInfo {
        private String userId;
        private String email;
        private long expirationTime;

        public Auth0UserInfo(@NonNull Object userObj, @NonNull Object emailObj, @NonNull Object expirationTimeObj) {
            this.userId = userObj.toString();
            this.email = emailObj.toString();
            this.expirationTime = Long.parseLong(expirationTimeObj.toString());
        }

        public String getUserId() {
            return this.userId;
        }

        public String getEmail() {
            return this.email;
        }

        public long getTokenExpiration() {
            return this.expirationTime;
        }
    }
}
