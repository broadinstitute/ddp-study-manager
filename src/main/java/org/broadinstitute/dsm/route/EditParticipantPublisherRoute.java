package org.broadinstitute.dsm.route;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.ddp.handlers.util.Result;
import org.broadinstitute.dsm.pubsub.EditParticipantMessagePublisher;
import org.broadinstitute.dsm.security.RequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;

import java.util.HashMap;
import java.util.Map;

public class EditParticipantPublisherRoute extends RequestHandler {

    private static final Logger logger = LoggerFactory.getLogger(EditParticipantPublisherRoute.class);

    private final String projectId;
    private final String topicId;

    public EditParticipantPublisherRoute(String projectId, String topicId) {
        this.projectId = projectId;
        this.topicId = topicId;
    }

    @Override
    public Object processRequest(Request request, Response response, String userId) throws Exception {

        String messageData = request.body();

        if (StringUtils.isBlank(messageData)) {
            logger.error("Message data is blank");
        }

        JsonObject messageJsonObject = new Gson().fromJson(messageData, JsonObject.class);

        JsonObject dataFromJson = messageJsonObject.get("data").getAsJsonObject();

        dataFromJson.getAsJsonObject().addProperty("resultType", "SUCCESS");

        String data = dataFromJson.toString();

        Map<String, String> attributeMap = getStringStringMap(userId, messageJsonObject);

        try {
            EditParticipantMessagePublisher.publishMessage(data, attributeMap, projectId, topicId);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new Result(200);
    }

    public Map<String, String> getStringStringMap(String userId, JsonObject messageJsonObject) {
        String participantGuid = messageJsonObject.get("participantGuid").getAsString();
        String studyGuid = messageJsonObject.get("studyGuid").getAsString();
        Map<String, String> attributeMap = new HashMap<>();
        attributeMap.put("taskType", "UPDATE_PROFILE");
        attributeMap.put("userId", userId);
        attributeMap.put("participantGuid", participantGuid);
        attributeMap.put("studyGuid", studyGuid);
        return attributeMap;
    }
}
