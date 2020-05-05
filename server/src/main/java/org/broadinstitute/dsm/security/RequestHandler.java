package org.broadinstitute.dsm.security;

import lombok.Getter;
import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.ddp.handlers.util.Result;
import org.broadinstitute.dsm.statics.RoutePath;
import org.broadinstitute.dsm.statics.UserErrorMessages;
import org.broadinstitute.dsm.util.Auth0Util;
import org.broadinstitute.dsm.util.SecurityUtil;
import org.broadinstitute.dsm.util.UserUtil;
import spark.QueryParamsMap;
import spark.Request;
import spark.Response;
import spark.Route;

import java.util.List;

@Getter
public abstract class RequestHandler implements Route {

    private String permission;
    private String userId;
    private String userMail;
    private String realm = null;
    protected Auth0Util auth0Util;

    public RequestHandler(@NonNull Auth0Util auth0Util, @NonNull String permission) {
        this.auth0Util = auth0Util;
        this.permission = permission;
    }

    @Override
    public Object handle(Request request, Response response) throws Exception {
        //get userId and userMail from token
        userId = SecurityUtil.getUserId(request);
        userMail = SecurityUtil.getUserMail(request);
        if (StringUtils.isNotBlank(userId) && StringUtils.isNotBlank(userMail)) {
            QueryParamsMap queryParams = request.queryMap();

            //check that sent userId is same in queryMap as in token
            String userIdRequest = null;
            if (queryParams.value(UserUtil.USER_ID) != null) {
                userIdRequest = queryParams.get(UserUtil.USER_ID).value();
                if (!userId.equals(userIdRequest)) {
                    throw new RuntimeException("User id was not equal. User Id in token " + userId + " user Id in request " + userIdRequest);
                }
            }

            if (queryParams.value(RoutePath.REALM) != null) {
                realm = queryParams.get(RoutePath.REALM).value();
            }

            //check actual permission
            if (checkPermissions(permission, userId, userMail, realm)) {
                return processRequest(request, response, userId, userMail);
            }
            else {
                return new Result(500, UserErrorMessages.NO_RIGHTS);
            }
        }
        throw new RuntimeException("Error user_id was missing from token");
    }

    private boolean checkPermissions(@NonNull String permission, @NonNull String userId, @NonNull String userMail) {
        return checkPermissions(permission, userId, userMail, null);
    }

    private boolean checkPermissions(@NonNull String permission, @NonNull String userId, @NonNull String userMail, String realm) {
        List<String> permissions = auth0Util.getUserPermissions(userId, userMail, realm);
        if (permissions != null && !permissions.isEmpty() && permissions.contains(permission)) {
            return true;
        }
        return false;
    }

    protected abstract Object processRequest(Request request, Response response, String userId, String userMail) throws Exception;
}
