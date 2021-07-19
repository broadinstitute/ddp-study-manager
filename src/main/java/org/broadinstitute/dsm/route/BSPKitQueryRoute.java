package org.broadinstitute.dsm.route;

import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.dsm.db.dao.kit.BSPKitDao;
import org.broadinstitute.dsm.statics.RequestParameter;
import org.broadinstitute.dsm.util.NotificationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;
import spark.Route;

public class BSPKitQueryRoute implements Route {

    private static final Logger logger = LoggerFactory.getLogger(BSPKitQueryRoute.class);

    private NotificationUtil notificationUtil;

    public BSPKitQueryRoute(@NonNull NotificationUtil notificationUtil) {
        this.notificationUtil = notificationUtil;
    }

    @Override
    public Object handle(Request request, Response response) throws Exception {
        String kitLabel = request.params(RequestParameter.LABEL);
        if (StringUtils.isBlank(kitLabel)) {
            throw new RuntimeException("Please include a kit label as a path parameter");
        }

        return  new BSPKitDao().getBSPKitInfo(kitLabel, response, this.notificationUtil);
    }
}
