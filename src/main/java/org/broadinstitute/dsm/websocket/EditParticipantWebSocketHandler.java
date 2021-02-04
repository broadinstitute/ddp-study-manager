package org.broadinstitute.dsm.websocket;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.broadinstitute.dsm.pubsub.EditParticipantMessagePublisher;
import org.eclipse.jetty.websocket.api.*;
import org.eclipse.jetty.websocket.api.annotations.*;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

@WebSocket
public class EditParticipantWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(EditParticipantWebSocketHandler.class);

    private String projectId;
    private String topicId;

    private static final Map<String, Session> sessionHashMap = new ConcurrentHashMap<>();

    public EditParticipantWebSocketHandler(String projectId, String topicId) {
        this.projectId = projectId;
        this.topicId = topicId;
    }

    @OnWebSocketConnect
    public void connected(Session session) {
        String userId = getUserId(session);
        sessionHashMap.put(userId, session);
    }


    @OnWebSocketClose
    public void closed(Session session, int statusCode, String reason) {
        String userId = getUserId(session);
        sessionHashMap.remove(userId);
    }

    @OnWebSocketMessage
    public void message(Session session, String message) throws IOException {
        logger.info("Got: " + message);   // Print message
        JsonObject messageJsonObject = new Gson().fromJson(message, JsonObject.class);

        String data = messageJsonObject.get("data").toString();

        Map<String, String> attributeMap = getStringStringMap(session, messageJsonObject);

        try {
            EditParticipantMessagePublisher.publishMessage(data, attributeMap, projectId, topicId);
        } catch (Exception e) {
            e.printStackTrace();
        }
        //session.getRemote().sendString(message); // and send it back
    }

    public Map<String, String> getStringStringMap(Session session, JsonObject messageJsonObject) {
        String participantGuid = messageJsonObject.get("participantGuid").getAsString();
        String studyGuid = messageJsonObject.get("studyGuid").getAsString();
        Map<String, String> attributeMap = new HashMap<>();
        attributeMap.put("type", "UPDATE_PROFILE");
        attributeMap.put("userId", getUserId(session));
        attributeMap.put("participantGuid", participantGuid);
        attributeMap.put("studyGuid", studyGuid);
        return attributeMap;
    }

    public static Map<String, Session> getSessionHashMap() {
        return sessionHashMap;
    }

    private static String getUserId(Session session) {
        String access_token = session.getUpgradeRequest().getParameterMap().get("token").get(0);
        Base64.Decoder decoder = Base64.getUrlDecoder();
        String[] parts = access_token.split("\\."); // split out the "parts" (header, payload and signature)
        String payloadJson = new String(decoder.decode(parts[1]));
        Gson gson = new Gson(); // Or use new GsonBuilder().create();
        JsonObject target = gson.fromJson(payloadJson, JsonObject.class);
        return target.get("USER_ID").getAsString();
    }
}

