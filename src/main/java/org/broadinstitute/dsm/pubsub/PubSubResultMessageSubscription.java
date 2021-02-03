package org.broadinstitute.dsm.pubsub;

import com.google.api.gax.batching.FlowControlSettings;
import com.google.cloud.pubsub.v1.AckReplyConsumer;
import com.google.cloud.pubsub.v1.MessageReceiver;
import com.google.cloud.pubsub.v1.Subscriber;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.pubsub.v1.ProjectSubscriptionName;
import com.google.pubsub.v1.PubsubMessage;
import org.broadinstitute.dsm.websocket.EditParticipantWebSocketHandler;
import org.eclipse.jetty.websocket.api.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class PubSubResultMessageSubscription {

    private static final Logger logger = LoggerFactory.getLogger(PubSubResultMessageSubscription.class);

    public static void dssToDsmSubscriber(String projectId, String subscriptionId) throws Exception {
        subscribeWithFlowControlSettings(projectId, subscriptionId);
    }

    public static void subscribeWithFlowControlSettings (
            String projectId, String subscriptionId) {
        ProjectSubscriptionName subscriptionName =
                ProjectSubscriptionName.of(projectId, subscriptionId);

        // Instantiate an asynchronous message receiver.
        MessageReceiver receiver =
                (PubsubMessage pubsubMessage, AckReplyConsumer consumer) -> {
                    // Handle incoming message, then ack the received message.
                    logger.info("Id: " + pubsubMessage.getMessageId());
                    logger.info("Data: " + pubsubMessage.getData().toStringUtf8());
                    String message = pubsubMessage.getData().toStringUtf8();
                    String userId = new Gson().fromJson(message, JsonObject.class).get("userId").getAsString();

                    try {
                        Session session = EditParticipantWebSocketHandler.getSessionHashMap().get(userId);
                        if (session != null) {
                            session.getRemote().sendString(message);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    consumer.ack();
                };

        Subscriber subscriber = null;

        // The subscriber will pause the message stream and stop receiving more messsages from the
        // server if any one of the conditions is met.
        FlowControlSettings flowControlSettings =
                FlowControlSettings.newBuilder()
                        // 1,000 outstanding messages. Must be >0. It controls the maximum number of messages
                        // the subscriber receives before pausing the message stream.
                        .setMaxOutstandingElementCount(1000L)
                        // 100 MiB. Must be >0. It controls the maximum size of messages the subscriber
                        // receives before pausing the message stream.
                        .setMaxOutstandingRequestBytes(100L * 1024L * 1024L)
                        .build();

        try {
            subscriber =
                    Subscriber.newBuilder(subscriptionName, receiver)
                            .setFlowControlSettings(flowControlSettings)
                            .build();

            // Start the subscriber.
            subscriber.startAsync().awaitRunning();
            // Allow the subscriber to run for 30s unless an unrecoverable error occurs.
            subscriber.awaitTerminated(30, TimeUnit.SECONDS);
        } catch (TimeoutException timeoutException) {
            // Shut down the subscriber after 30s. Stop receiving messages.
            subscriber.stopAsync();
        }
    }
}
