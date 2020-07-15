package org.broadinstitute.dsm.route;

import com.google.gson.Gson;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.dsm.db.DDPInstance;
import org.broadinstitute.dsm.db.KitRequestShipping;
import org.broadinstitute.dsm.statics.RequestParameter;
import org.broadinstitute.dsm.util.SystemUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;
import spark.Route;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BatchKitsRoute implements Route {
    private static final Logger logger = LoggerFactory.getLogger(BatchKitsRoute.class);

    @Override
    public Object handle(Request request, Response response) throws Exception {
        String study = request.params(RequestParameter.REALM);
        String ids = null;
        if (StringUtils.isNotBlank(study)) {
            HttpServletRequest rawRequest = request.raw();
            String content = SystemUtil.getBody(rawRequest);
            ids = (String) (new Gson().fromJson(content, new HashMap<String, String[]>().getClass())).get("participantIds");
            String[] ddpParticipantIds = new Gson().fromJson(ids, String[].class);
            DDPInstance ddpInstance = DDPInstance.getDDPInstance(study);
            if (ddpInstance != null) {
                if (ddpParticipantIds != null && ddpParticipantIds.length != 0) {
                    Map<String, List<KitRequestShipping>> kitRequests = KitRequestShipping.getKitRequests(ddpInstance.getName());
                    Map<String, List<KitRequestShipping>> results = new HashMap<>();
                    for (String ddpParticipantId : ddpParticipantIds) {
                        results.put(ddpParticipantId, kitRequests.getOrDefault(ddpParticipantId, new ArrayList<KitRequestShipping>()));
                    }
                    logger.info("Sending a list of "+results.size()+" KitRequestShippings for study "+study);
                    return results;
                }
                else {
                    return null;
                }
            }
            logger.error("No study found for: " + study);
            response.status(404);
            return null;
        }
        logger.error("No value was sent for realm");
        response.status(500);
        return null;
    }
}
