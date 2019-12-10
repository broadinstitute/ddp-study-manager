package org.broadinstitute.dsm.route;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.ddp.handlers.util.Result;
import org.broadinstitute.dsm.DSMServer;
import org.broadinstitute.dsm.db.KitRequestShipping;
import org.broadinstitute.dsm.security.RequestHandler;
import org.broadinstitute.dsm.statics.RequestParameter;
import org.broadinstitute.dsm.statics.UserErrorMessages;
import org.broadinstitute.dsm.util.DDPRequestUtil;
import org.broadinstitute.dsm.util.UserUtil;
import spark.Request;
import spark.Response;

public class KitDeactivationRoute extends RequestHandler {

    private final DDPRequestUtil ddpRequestUtil;

    public KitDeactivationRoute(@NonNull DDPRequestUtil ddpRequestUtil) {
        this.ddpRequestUtil = ddpRequestUtil;
    }

    @Override
    public Object processRequest(Request request, Response response, String userId) throws Exception {
        String kitRequestId = request.params(RequestParameter.KITREQUESTID);
        if (StringUtils.isNotBlank(kitRequestId)) {
            boolean deactivate = request.url().toLowerCase().contains("deactivate");
            KitRequestShipping kitRequest = KitRequestShipping.getKitRequest(kitRequestId);
            String realm = kitRequest.getRealm();
            if (UserUtil.checkUserAccess(realm, userId, "kit_deactivation")) {
                if (deactivate) {
                    String userIdRequest = UserUtil.getUserId(request);
                    if (!userId.equals(userIdRequest)) {
                        throw new RuntimeException("User id was not equal. User Id in token " + userId + " user Id in request " + userIdRequest);
                    }
                    JsonObject jsonObject = new JsonParser().parse(request.body()).getAsJsonObject();
                    String reason = jsonObject.get("reason").getAsString();
                    KitRequestShipping.deactivateKitRequest(kitRequestId, reason, DSMServer.getDDPEasypostApiKey(realm), userIdRequest);
                }
                else {
                    KitRequestShipping.reactivateKitRequest(kitRequestId);
                }
                return new Result(200);
            }
            else {
                response.status(500);
                return new Result(500, UserErrorMessages.NO_RIGHTS);
            }
        }
        else {
            throw new RuntimeException("KitRequestId id was missing");
        }
    }
}
