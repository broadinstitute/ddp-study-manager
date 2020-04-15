package org.broadinstitute.dsm.util;

import com.auth0.client.auth.AuthAPI;
import com.auth0.client.mgmt.ManagementAPI;
import com.auth0.json.auth.TokenHolder;
import com.auth0.json.mgmt.Permission;
import com.auth0.json.mgmt.PermissionsPage;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
        if (account == null) {
            throw new NullPointerException("account");
        }
        else if (ddpKey == null) {
            throw new NullPointerException("ddpKey");
        }
        else if (ddpSecret == null) {
            throw new NullPointerException("ddpSecret");
        }
        else if (mgtKey == null) {
            throw new NullPointerException("mgtKey");
        }
        else if (mgtSecret == null) {
            throw new NullPointerException("mgtSecret");
        }
        else if (mgtApiUrl == null) {
            throw new NullPointerException("mgtApiUrl");
        }
        else {
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
        if (idToken == null) {
            throw new NullPointerException("idToken");
        }
        else {
            Map<String, Claim> auth0Claims = this.verifyAndParseAuth0TokenClaims(idToken);
            Auth0Util.Auth0UserInfo userInfo = new Auth0Util.Auth0UserInfo((auth0Claims.get("sub")).asString(), (auth0Claims.get("email")).asString(), (auth0Claims.get("exp")).asInt());
            return userInfo;
        }
    }

    public List<String> getUserPermissions(@NonNull String userId, @NonNull String email) {
        logger.info("Getting user permissions");
        if (userId == null) {
            throw new NullPointerException("userId");
        }
        else if (email == null) {
            throw new NullPointerException("email");
        }
        else {
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
                throw new RuntimeException("User connection verification failed for user " + email, e);
            }
        }
    }

    public Map<String, Claim> verifyAndParseAuth0TokenClaims(String auth0Token) {
        new HashMap();

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
            if (userObj == null) {
                throw new NullPointerException("userObj");
            }
            else if (emailObj == null) {
                throw new NullPointerException("emailObj");
            }
            else if (expirationTimeObj == null) {
                throw new NullPointerException("expirationTimeObj");
            }
            else {
                this.userId = userObj.toString();
                this.email = emailObj.toString();
                this.expirationTime = Long.parseLong(expirationTimeObj.toString());
            }
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
