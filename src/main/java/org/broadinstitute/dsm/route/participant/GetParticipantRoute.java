package org.broadinstitute.dsm.route.participant;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.dsm.db.DDPInstance;
import org.broadinstitute.dsm.model.participant.ParticipantWrapper;
import org.broadinstitute.dsm.security.RequestHandler;
import org.broadinstitute.dsm.statics.RoutePath;
import org.broadinstitute.dsm.util.ElasticSearchUtil;
import org.broadinstitute.dsm.util.ParticipantUtil;
import org.broadinstitute.dsm.util.UserUtil;
import spark.QueryParamsMap;
import spark.Request;
import spark.Response;

public class GetParticipantRoute extends RequestHandler {

    @Override
    protected Object processRequest(Request request, Response response, String userId) throws Exception {

        QueryParamsMap queryParamsMap = request.queryMap();

        String requestUserId = queryParamsMap.get(UserUtil.USER_ID).value();
        if (!userId.equals(requestUserId)) throw new IllegalAccessException("User id: " + userId  + "does not match to request user id: " + requestUserId);

        String realm = queryParamsMap.get(RoutePath.REALM).value();
        if (StringUtils.isBlank(realm)) throw new IllegalArgumentException("realm cannot be empty");

        DDPInstance ddpInstance = DDPInstance.getDDPInstance(realm);

        String ddpParticipantId = queryParamsMap.get(RoutePath.DDP_PARTICIPANT_ID).value();
        if (StringUtils.isBlank(ddpParticipantId)) throw new IllegalArgumentException("participant id cannot be empty");

        Map<String, String> queryConditions = Map.of(
                "p", " AND p.ddp_participant_id = '" + ddpParticipantId + "'",
                "ES", ParticipantUtil.isGuid(ddpParticipantId)
                        ? ElasticSearchUtil.BY_GUID + ddpParticipantId
                        : ElasticSearchUtil.BY_LEGACY_ALTPID + ddpParticipantId
        );
        return ParticipantWrapper.getFilteredList(ddpInstance, queryConditions);
    }
}
