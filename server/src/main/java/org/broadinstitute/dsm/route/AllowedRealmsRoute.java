package org.broadinstitute.dsm.route;

import org.broadinstitute.dsm.security.RequestHandler;
import org.broadinstitute.dsm.util.Auth0Util;
import org.broadinstitute.dsm.util.UserUtil;
import spark.Request;
import spark.Response;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class AllowedRealmsRoute extends RequestHandler {

    private static final String MENU = "menu";

    private Auth0Util auth0Util;

    public AllowedRealmsRoute(Auth0Util auth0Util) {
        this.auth0Util = auth0Util;
    }

    @Override
    public Object processRequest(Request request, Response response, String userId, String userMail) throws Exception {
        String userIdRequest = UserUtil.getUserId(request);
        if (!userId.equals(userIdRequest)) {
            throw new RuntimeException("User id was not equal. User Id in token " + userId + " user Id in request " + userIdRequest);
        }

        //get list of realms user can see
        List<String> permissions = auth0Util.getUserPermissions(userId, userMail);
        List<String> allowedRealms = permissions.stream()
                .filter(permission -> permission.startsWith("instance"))
                .map(permission -> Objects.toString(permission.replace("instance:",""), null))
                .collect(Collectors.toList());

        return allowedRealms;
    }
}
