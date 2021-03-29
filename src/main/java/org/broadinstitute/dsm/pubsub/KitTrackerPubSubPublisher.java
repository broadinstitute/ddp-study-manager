package org.broadinstitute.dsm.pubsub;

import com.google.cloud.pubsub.v1.Publisher;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.ProjectTopicName;
import com.google.pubsub.v1.PubsubMessage;
import com.google.pubsub.v1.TopicName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;


public class KitTrackerPubSubPublisher {

    private final Logger logger = LoggerFactory.getLogger(KitTrackerPubSubPublisher.class);

    public void publishMessage(String projectId, String topicId, String kits)
            throws IOException, InterruptedException {


        TopicName topicName = TopicName.of(projectId, topicId);
        // Create a publisher instance with default settings bound to the topic
        logger.info("Publishing message to topic: " + topicName);

        ByteString data = ByteString.copyFrom(kits, StandardCharsets.UTF_8);
        PubsubMessage pubsubMessage = PubsubMessage.newBuilder().setData(data).build();

        // Once published, returns a server-assigned message id (unique within the topic)
        Publisher publisher = Publisher.newBuilder(
                ProjectTopicName.of(projectId, topicId)).build();


        String responseMessage;
        try {
//            ApiFuture<String> future = publisher.publish(pubsubMessage);
            String messageId = publisher.publish(pubsubMessage).get();
            responseMessage = "Pubsub message published. MessageId: " + messageId;
        }
        catch (InterruptedException | ExecutionException e) {
            logger.error("Error publishing Pub/Sub message: " + e.getMessage(), e);
            responseMessage = "Error publishing Pub/Sub message; see logs for more info. ";
        }
        finally {
            publisher.shutdown();
        }

        logger.info(responseMessage + kits);

    }

}
