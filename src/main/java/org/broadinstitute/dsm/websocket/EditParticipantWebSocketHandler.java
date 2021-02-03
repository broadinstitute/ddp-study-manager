package org.broadinstitute.dsm.websocket;

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

    // Store sessions if you want to, for example, broadcast a message to all users
    private static final Queue<Session> sessions = new ConcurrentLinkedQueue<>();
    public static final Map<String, Session> sessionHashMap = new ConcurrentHashMap<>();

    public EditParticipantWebSocketHandler(String projectId, String topicId) {
        this.projectId = projectId;
        this.topicId = topicId;
    }

    @OnWebSocketConnect
    public void connected(Session session) {
        sessions.add(session);
    }

    @OnWebSocketClose
    public void closed(Session session, int statusCode, String reason) {
        sessions.remove(session);
    }

    @OnWebSocketMessage
    public void message(Session session, String message) throws IOException {
        logger.info("Got: " + message);   // Print message
        try {
            EditParticipantMessagePublisher.publishMessage(message, projectId, topicId);
        } catch (Exception e) {
            e.printStackTrace();
        }
        //session.getRemote().sendString(message); // and send it back
    }

}