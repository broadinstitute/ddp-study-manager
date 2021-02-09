package org.broadinstitute.dsm.route;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.dsm.pubsub.EditParticipantMessagePublisher;
import org.broadinstitute.dsm.security.RequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class EditParticipantPublisherRoute extends RequestHandler {

    private static final Logger logger = LoggerFactory.getLogger(EditParticipantPublisherRoute.class);

    private final String projectId;
    private final String topicId;

    private static final Map<String, String> messageHashMap = new ConcurrentHashMap<>();

    public static Map<String, String> getMessageHashMap() {
        return messageHashMap;
    }

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

        JsonObject data1 = messageJsonObject.get("data").getAsJsonObject();

        //data1.getAsJsonObject().addProperty("resultType", "SUCCESS");

        String data = data1.toString();



        Map<String, String> attributeMap = getStringStringMap(userId, messageJsonObject);

        try {
            EditParticipantMessagePublisher.publishMessage(data, attributeMap, projectId, topicId);
        } catch (Exception e) {
            e.printStackTrace();
        }
        TimeUnit.SECONDS.sleep(30);
        JsonObject jsonObject = new Gson().fromJson(messageHashMap.get(userId), JsonObject.class);
        return jsonObject.toString();

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
