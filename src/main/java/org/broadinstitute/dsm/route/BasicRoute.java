package org.broadinstitute.dsm.route;

import org.broadinstitute.dsm.security.RequestHandler;
import org.broadinstitute.dsm.statics.RoutePath;
import org.broadinstitute.dsm.util.UserUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.QueryParamsMap;
import spark.Request;
import spark.Response;

public class BasicRoute extends RequestHandler {

    private static final Logger logger = LoggerFactory.getLogger(DownloadPDFRoute.class);
    private UserUtil userUtil = new UserUtil();
    private String accessRole;

    public BasicRoute(String accessRole){
        this.accessRole = accessRole;
    }

    @Override
    protected Object processRequest(Request request, Response response, String userId) throws Exception {
        logger.info(request.url());
        QueryParamsMap queryParams = request.queryMap();
        String realm = null;
        if (queryParams.value(RoutePath.REALM) != null) {
            realm = queryParams.get(RoutePath.REALM).value();
        }
        UserUtil userUtil = new UserUtil();
        String userIdR = userUtil.getUserId(request);
        if (!userUtil.checkUserAccess(realm, userId, accessRole, userIdR)) {
            return 404;
        }
        return 200;
    }
}
