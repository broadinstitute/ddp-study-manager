package org.broadinstitute.dsm.route;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.ddp.security.SecurityHelper;
import org.broadinstitute.dsm.db.UserSettings;
import org.broadinstitute.dsm.model.auth.Access;
import org.broadinstitute.dsm.model.auth.AccessRole;
import org.broadinstitute.dsm.util.Auth0Util;
import org.broadinstitute.dsm.util.JWTRouteFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;
import spark.Route;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AuthenticationRoute implements Route {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationRoute.class);

    private final String payloadToken = "token";

    public static final String AUTH_USER_ID = "USER_ID";
    public static final String AUTH_USER_MAIL = "USER_MAIL";
    public static final String USER_ACCESS_ROLE = "USER_ACCESS_ROLE";
    private static final String USER_SETTINGS = "USER_SETTINGS";

    private final Auth0Util auth0Util;

    private final String jwtSecret;

    public AuthenticationRoute(@NonNull Auth0Util auth0Util, @NonNull String jwtSecret) {
        if (StringUtils.isBlank(jwtSecret)) {
            throw new RuntimeException("Browser security information is missing");
        }
        this.auth0Util = auth0Util;
        this.jwtSecret = jwtSecret;
    }

    @Override
    public Object handle(Request request, Response response) throws Exception {
        logger.info("Check user...");
        JsonObject jsonObject = new JsonParser().parse(request.body()).getAsJsonObject();
        String auth0Token = jsonObject.get(payloadToken).getAsString();
        if (StringUtils.isNotBlank(auth0Token)) {
            // checking if Auth0 knows that token
            Auth0Util.Auth0UserInfo auth0UserInfo = auth0Util.getAuth0UserInfo(auth0Token);
            if (auth0UserInfo != null) {
                return createNewDSMToken(auth0Util, jwtSecret, auth0UserInfo);
            }
            else {
                throw new RuntimeException("UserIdentity not found");
            }
        }
        else {
            throw new RuntimeException("There was no token in the payload");
        }
    }

    public static DSMToken createNewDSMToken(@NonNull Auth0Util auth0Util, @NonNull String jwtSecret, @NonNull Auth0Util.Auth0UserInfo auth0UserInfo) {
        String email = auth0UserInfo.getEmail();
        logger.info("User (" + email + ") was found ");
        Gson gson = new Gson();
        Map<String, String> claims = new HashMap<>();
        claims.put(AUTH_USER_ID, auth0UserInfo.getUserId());
        claims.put(AUTH_USER_MAIL, auth0UserInfo.getEmail());
        claims.put(USER_SETTINGS, gson.toJson(UserSettings.getUserSettings(auth0UserInfo.getUserId(), auth0UserInfo.getEmail()), UserSettings.class));
        List<AccessRole> accessRoles = auth0Util.getAccess(auth0UserInfo.getUserId(), auth0UserInfo.getEmail());
        long exp = (System.currentTimeMillis() / 1000) + (60 * 5); //5 min from now
        Access access = new Access(exp, accessRoles);
        claims.put(USER_ACCESS_ROLE, gson.toJson(access, Access.class));

        long auth0Expiration = auth0UserInfo.getTokenExpiration();
        int cookieAgeInSeconds = new Long(auth0Expiration - new Double(System.currentTimeMillis() / 1000d).intValue()).intValue();

        String jwtToken = new SecurityHelper().createToken(jwtSecret, cookieAgeInSeconds + (System.currentTimeMillis() / 1000) + (60 * 5), claims);

        DSMToken authResponse = new DSMToken(jwtToken);
        return authResponse;
    }

    public static DSMToken checkToken(@NonNull Request request, @NonNull String jwtSecret, @NonNull Auth0Util auth0Util) {
        String authHeader = request.headers(JWTRouteFilter.AUTHORIZATION);
        if (StringUtils.isNotBlank(authHeader)) {
            String[] parsedAuthHeader = authHeader.split(JWTRouteFilter.BEARER);
            if (parsedAuthHeader != null) {
                if (parsedAuthHeader.length == 2) {
                    String jwtToken = parsedAuthHeader[1].trim();
                    if (StringUtils.isNotBlank(jwtToken)) {
                        try {
                            Algorithm algorithm = Algorithm.HMAC256(jwtSecret);
                            JWTVerifier verifier = JWT.require(algorithm).build(); //Reusable verifier instance
                            DecodedJWT jwt = verifier.verify(jwtToken);

                            Map<String, Claim> claims = jwt.getClaims();
                            if (claims != null) {
                                if (claims.containsKey(USER_ACCESS_ROLE)) {
                                    Object accessObj = claims.get(USER_ACCESS_ROLE);
                                    Object userIdObj = claims.get(AUTH_USER_ID);
                                    Object userMailObj = claims.get(AUTH_USER_MAIL);
                                    if (accessObj != null && userIdObj != null && userMailObj != null) {
                                        Access access = new Gson().fromJson(((Claim) accessObj).asString(), Access.class);
                                        long exp = access.getExp();
                                        if ((System.currentTimeMillis() / 1000d) >= exp) {
                                            //create new token
                                            return createNewDSMToken(auth0Util, jwtToken, new Auth0Util.Auth0UserInfo(((Claim) userIdObj).asString(), ((Claim) userMailObj).asString(), jwt.getExpiresAt().getTime()));
                                        }
                                    }
                                }
                            }
                        }
                        catch (Exception e) {
                            logger.error("Invalid token: " + jwtToken, e);
                        }
                    }
                }
            }
        }
        return null;
    }

    private static class DSMToken {
        private String dsmToken;

        public DSMToken(String token) {
            this.dsmToken = token;
        }
    }
}
