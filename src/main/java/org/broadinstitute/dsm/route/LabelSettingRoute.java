package org.broadinstitute.dsm.route;

import com.google.gson.Gson;
import lombok.NonNull;
import org.broadinstitute.ddp.handlers.util.Result;
import org.broadinstitute.dsm.db.LabelSettings;
import org.broadinstitute.dsm.security.RequestHandler;
import org.broadinstitute.dsm.statics.RoutePath;
import org.broadinstitute.dsm.statics.UserErrorMessages;
import org.broadinstitute.dsm.util.Auth0Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;

public class LabelSettingRoute extends RequestHandler {

    private static final Logger logger = LoggerFactory.getLogger(LabelSettingRoute.class);

    private static final String PERMISSION = "mr:shipping";//TODO two roles @ GET `kit_shipping_view`

    public LabelSettingRoute(@NonNull Auth0Util auth0Util) {
        super(auth0Util, PERMISSION);
    }

    @Override
    public Object processRequest(Request request, Response response, String userId, String userMail) throws Exception {
        if (RoutePath.RequestMethod.GET.toString().equals(request.requestMethod())) {
            return LabelSettings.getLabelSettings();
        }
        if (RoutePath.RequestMethod.PATCH.toString().equals(request.requestMethod())) {
            String requestBody = request.body();
            LabelSettings[] labelSettings = new Gson().fromJson(requestBody, LabelSettings[].class);
            LabelSettings.saveLabelSettings(labelSettings);
            return new Result(200);
        }
        logger.error("Request method not known");
        return new Result(500, UserErrorMessages.CONTACT_DEVELOPER);
    }
}
