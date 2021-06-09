package org.broadinstitute.dsm.pubsub;

import com.google.api.gax.core.ExecutorProvider;
import com.google.api.gax.core.InstantiatingExecutorProvider;
import com.google.cloud.pubsub.v1.AckReplyConsumer;
import com.google.cloud.pubsub.v1.MessageReceiver;
import com.google.cloud.pubsub.v1.Subscriber;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.pubsub.v1.ProjectSubscriptionName;
import com.google.pubsub.v1.PubsubMessage;
import org.broadinstitute.dsm.db.DDPInstance;
import org.broadinstitute.dsm.db.dao.participant.data.ParticipantDataDao;
import org.broadinstitute.dsm.db.dto.participant.data.ParticipantDataDto;
import org.broadinstitute.dsm.util.ElasticSearchUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class UpdateWorkflowStatusSubscription {

    private static final Logger logger = LoggerFactory.getLogger(ElasticExportSubscription.class);
    private static final Gson gson = new Gson();
    private static final ParticipantDataDao participantDataDao = new ParticipantDataDao();
    public static final String STUDY_GUID = "studyGuid";
    public static final String PARTICIPANT_GUID = "participantGuid";
    public static final String MEMBER_TYPE = "MEMBER_TYPE";
    public static final String SELF = "SELF";
    public static final String DSS = "DSS";
    public static final String RGP = "RGP";

    public static void subscribeUpdateWorkflow(String projectId, String subscriptionId) {
        // Instantiate an asynchronous message receiver.
        MessageReceiver receiver =
                (PubsubMessage message, AckReplyConsumer consumer) -> {
                    // Handle incoming message, then ack the received message.
                    logger.info("Got message with Id: " + message.getMessageId());
                    consumer.ack();
                    String data = message.getData() != null ? message.getData().toStringUtf8() : null;
                    WorkflowPayload payload = gson.fromJson(data, UpdateWorkflowStatusSubscription.WorkflowPayload.class);
                    String workflow = payload.getWorkflow();
                    String status = payload.getStatus();

                    Map<String, String> attributesMap = message.getAttributesMap();
                    String studyGuid = attributesMap.get(STUDY_GUID);
                    String ddpParticipantId = attributesMap.get(PARTICIPANT_GUID);
                    DDPInstance instance = DDPInstance.getDDPInstanceByGuid(studyGuid);

                    List<ParticipantDataDto> participantDatas = participantDataDao.getParticipantDataByParticipantId(ddpParticipantId);

                    participantDatas.forEach(participantDataDto -> {
                        updateProbandStatusInDB(workflow, status, participantDataDto, studyGuid);
                    });

                    ElasticSearchUtil.writeWorkflow(instance, ddpParticipantId, workflow, status);
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
            logger.info("Started pubsub subscription receiver for updating workflow statuses");
        } catch (TimeoutException e) {
            throw new RuntimeException("Timed out while starting pubsub subscription for updating workflow statuses", e);
        }
    }

    public static void updateProbandStatusInDB(String workflow, String status, ParticipantDataDto participantDataDto, String studyGuid) {
        String oldData = participantDataDto.getData();
        if (oldData != null) {
            JsonObject dataJsonObject = gson.fromJson(oldData, JsonObject.class);
            if (!RGP.equals(studyGuid) || isProband(dataJsonObject)) {
                dataJsonObject.addProperty(workflow, status);
                participantDataDao.updateParticipantDataColumn(
                        new ParticipantDataDto(participantDataDto.getParticipantDataId(),
                                participantDataDto.getDdpParticipantId(),
                                participantDataDto.getDdpInstanceId(),
                                participantDataDto.getFieldTypeId(),
                                dataJsonObject.toString(),
                                System.currentTimeMillis(), DSS)
                );
            }
        }
    }

    private static boolean isProband(JsonObject dataJsonObject) {
        return dataJsonObject.has(MEMBER_TYPE) && dataJsonObject.get(MEMBER_TYPE).getAsString().equals(SELF);
    }

    public static class WorkflowPayload {
        private String workflow;
        private String status;

        public String getWorkflow() {
            return workflow;
        }

        public String getStatus() {
            return status;
        }
    }
}
