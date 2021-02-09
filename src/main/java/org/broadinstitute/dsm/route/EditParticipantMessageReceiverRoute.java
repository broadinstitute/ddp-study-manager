package org.broadinstitute.dsm.route;

import org.broadinstitute.ddp.handlers.util.Result;
import org.broadinstitute.dsm.db.EditParticipantMessage;
import org.broadinstitute.dsm.security.RequestHandler;
import org.broadinstitute.dsm.statics.DBConstants;
import org.broadinstitute.dsm.statics.RoutePath;
import org.broadinstitute.dsm.statics.UserErrorMessages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;

import java.util.List;
import java.util.Map;

public class EditParticipantMessageReceiverRoute extends RequestHandler {

    private static final Logger logger = LoggerFactory.getLogger(EditParticipantPublisherRoute.class);

    @Override
    public Object processRequest(Request request, Response response, String userId) throws Exception {
        if (RoutePath.RequestMethod.GET.toString().equals(request.requestMethod())) {
            List<String> messageWithStatus = EditParticipantMessage.getMessageWithStatus(userId);
            String status = messageWithStatus.get(0);
            String message = messageWithStatus.get(1);
            if (DBConstants.MESSAGE_RECEIVED_STATUS.equals(status)) {
                return message;
            }
        }
        logger.error("Request method not known");
        return new Result(500, UserErrorMessages.CONTACT_DEVELOPER);
    }
}
