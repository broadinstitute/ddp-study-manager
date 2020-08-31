package org.broadinstitute.dsm.jobs;

import com.google.cloud.pubsub.v1.AckReplyConsumer;
import com.google.cloud.pubsub.v1.MessageReceiver;
import com.google.cloud.pubsub.v1.Subscriber;
import com.google.gson.Gson;
import com.google.pubsub.v1.ProjectSubscriptionName;
import com.google.pubsub.v1.PubsubMessage;
import org.broadinstitute.dsm.model.birch.TestBostonResult;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class PubSubLookUpJob implements Job {
    private static final Logger logger = LoggerFactory.getLogger(PubSubLookUpJob.class);
    private static String SQL_INSERT_RESULTS = "INSERT INTO ddp_kit_request ";
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        String projectId = "hl7-dev-274911";
        String subscriptionId = "pegah-results-sub";

        try{
            ProjectSubscriptionName subscriptionName =
                    ProjectSubscriptionName.of(projectId, subscriptionId);

            // Instantiate an asynchronous message receiver.
            MessageReceiver receiver =
                    (PubsubMessage message, AckReplyConsumer consumer) -> {
                        // Handle incoming message, then ack the received message.
                        System.out.println("Id: " + message.getMessageId());
                        System.out.println("Data: " + message.getData().toStringUtf8());
                        consumer.ack();
                    };

            Subscriber subscriber = null;
            try {
                subscriber = Subscriber.newBuilder(subscriptionName, receiver).build();
                // Start the subscriber.
                subscriber.startAsync().awaitRunning();
                logger.info("Listening for messages on %s:\n", subscriptionName.toString());
                // Allow the subscriber to run for 30s unless an unrecoverable error occurs.
                subscriber.awaitTerminated(1L, TimeUnit.MINUTES);
            } catch (TimeoutException timeoutException) {
                // Shut down the subscriber after 30s. Stop receiving messages.
                subscriber.stopAsync();
            }
        }
        catch (Exception e){
            throw new RuntimeException("Failed to get results from pubsub ", e);
        }

    }

    public static void writeResultsIntoDB(PubsubMessage message){
        String data = message.getData().toStringUtf8();
        TestBostonResult testBostonResult = new Gson().fromJson(data, TestBostonResult.class);
        testBostonResult = new Gson().fromJson(message.getData().toString(), TestBostonResult.class);

    }
}
