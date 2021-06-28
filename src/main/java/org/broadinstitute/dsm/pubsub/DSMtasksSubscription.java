package org.broadinstitute.dsm.pubsub;

import com.google.api.gax.core.ExecutorProvider;
import com.google.api.gax.core.InstantiatingExecutorProvider;
import com.google.cloud.pubsub.v1.AckReplyConsumer;
import com.google.cloud.pubsub.v1.MessageReceiver;
import com.google.cloud.pubsub.v1.Subscriber;
import com.google.pubsub.v1.ProjectSubscriptionName;
import com.google.pubsub.v1.PubsubMessage;
import org.broadinstitute.dsm.export.ExportToES;
import org.broadinstitute.dsm.model.defaultvalues.Defaultable;
import org.broadinstitute.dsm.model.defaultvalues.DefaultableMaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class DSMtasksSubscription {

    private static final Logger logger = LoggerFactory.getLogger(DSMtasksSubscription.class);
    public static final String TASK_TYPE = "taskType";
    public static final String UPDATE_CUSTOM_WORKFLOW = "UPDATE_CUSTOM_WORKFLOW";
    public static final String ELASTIC_EXPORT = "ELASTIC_EXPORT";
    public static final String PARTICIPANT_REGISTERED = "PARTICIPANT_REGISTERED";

    public static void subscribeDSMtasks(String projectId, String subscriptionId) {
        // Instantiate an asynchronous message receiver.
        MessageReceiver receiver =
                (PubsubMessage message, AckReplyConsumer consumer) -> {
                    // Handle incoming message, then ack the received message.
                    logger.info("Got message with Id: " + message.getMessageId());
                    consumer.ack();
                    Map<String, String> attributesMap = message.getAttributesMap();
                    String taskType = attributesMap.get(TASK_TYPE);
                    String data = message.getData() != null ? message.getData().toStringUtf8() : null;

                    logger.info("Task type is: " + taskType);

                    switch (taskType) {
                        case UPDATE_CUSTOM_WORKFLOW:
                            WorkflowStatusUpdate.updateCustomWorkflow(attributesMap, data);
                            break;
                        case ELASTIC_EXPORT:
                            ExportToES.exportObjectsToES(data);
                            break;
                        case PARTICIPANT_REGISTERED:
                            String studyGuid = attributesMap.get("studyGuid");
                            String participantGuid = attributesMap.get("participantGuid");
                            Defaultable defaultable = DefaultableMaker
                                    .makeDefaultable(Enum.valueOf(DefaultableMaker.Study.class, studyGuid.toUpperCase()));
                            boolean result = defaultable.generateDefaults(studyGuid, participantGuid);
                            if (!result) consumer.nack();
                            break;
                        default:
                            logger.warn("Wrong task type for a message from pubsub");
                            break;
                    }
                };

        Subscriber subscriber = null;
        ProjectSubscriptionName resultSubName = ProjectSubscriptionName.of(projectId, subscriptionId);
        ExecutorProvider resultsSubExecProvider = InstantiatingExecutorProvider.newBuilder().setExecutorThreadCount(1).build();
        subscriber = Subscriber.newBuilder(resultSubName, receiver)
                .setParallelPullCount(1)
                .setExecutorProvider(resultsSubExecProvider)
                .setMaxAckExtensionPeriod(org.threeten.bp.Duration.ofSeconds(120))
                .build();
        try {
            subscriber.startAsync().awaitRunning(1L, TimeUnit.MINUTES);
            logger.info("Started pubsub subscription receiver DSM tasks subscription");
        } catch (TimeoutException e) {
            throw new RuntimeException("Timed out while starting pubsub subscription for DSM tasks", e);
        }
    }
}
