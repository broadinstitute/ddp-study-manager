package org.broadinstitute.dsm.route.participant;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.dsm.db.DDPInstance;
import org.broadinstitute.dsm.db.dao.ddp.instance.DDPInstanceDao;
import org.broadinstitute.dsm.security.RequestHandler;
import org.broadinstitute.dsm.util.ElasticSearchUtil;
import spark.QueryParamsMap;
import spark.Request;
import spark.Response;

public class NumberOfParticipantsRoute extends RequestHandler {
    @Override
    protected Object processRequest(Request request, Response response, String userId) throws Exception {
        QueryParamsMap queryParamsMap = request.queryMap();
        String realm = queryParamsMap.get("realm").value();
        if (StringUtils.isBlank(realm)) throw new RuntimeException("Realm is not provided");
        DDPInstance ddpInstanceByRealm = DDPInstanceDao.getDDPInstanceByRealm(realm);
        return ElasticSearchUtil.getAmountOfParticipants(realm, ddpInstanceByRealm.getParticipantIndexES());
    }
}
