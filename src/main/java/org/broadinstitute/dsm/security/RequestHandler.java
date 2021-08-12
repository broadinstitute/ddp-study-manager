package org.broadinstitute.dsm.security;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.dsm.statics.RoutePath;
import org.broadinstitute.dsm.statics.UserErrorMessages;
import org.broadinstitute.dsm.util.SecurityUtil;
import org.broadinstitute.dsm.util.UserUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.QueryParamsMap;
import spark.Request;
import spark.Response;
import spark.Route;

public abstract class RequestHandler implements Route {

    private UserUtil userUtil = new UserUtil();
    private String accessRole;
    private static final Logger logger = LoggerFactory.getLogger(RequestHandler.class);
    public RequestHandler(String accessRole){
        this.accessRole = accessRole;
    }

    @Override
    public Object handle(Request request, Response response) throws Exception {
        //get the token if it is there
        String userId = SecurityUtil.getUserId(request);
        logger.info(request.url());
        QueryParamsMap queryParams = request.queryMap();
        String realm = null;
        if (queryParams.value(RoutePath.REALM) != null) {
            realm = queryParams.get(RoutePath.REALM).value();
        }
        String userIdR = userUtil.getUserId(request);
        if (!userUtil.checkUserAccess(realm, userId, accessRole, userIdR)) {
            response.status(500);
            return UserErrorMessages.NO_RIGHTS;
        }
        if (StringUtils.isNotBlank(userId)) {
            return processRequest(request, response, userId);
        }
        throw new RuntimeException("Error user_id was missing from token");
    }

    protected abstract Object processRequest(Request request, Response response, String userId) throws Exception;
}
