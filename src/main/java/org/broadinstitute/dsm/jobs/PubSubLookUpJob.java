package org.broadinstitute.dsm.jobs;

import com.google.api.gax.core.ExecutorProvider;
import com.google.api.gax.core.InstantiatingExecutorProvider;
import com.google.cloud.pubsub.v1.AckReplyConsumer;
import com.google.cloud.pubsub.v1.MessageReceiver;
import com.google.cloud.pubsub.v1.Subscriber;
import com.google.gson.Gson;
import com.google.pubsub.v1.ProjectSubscriptionName;
import com.google.pubsub.v1.PubsubMessage;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.ddp.db.SimpleResult;
import org.broadinstitute.dsm.model.KitDDPNotification;
import org.broadinstitute.dsm.model.birch.TestBostonResult;
import org.broadinstitute.dsm.util.EventUtil;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.threeten.bp.Duration;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.broadinstitute.ddp.db.TransactionWrapper.inTransaction;

public class PubSubLookUpJob implements Job {
    private static final Logger logger = LoggerFactory.getLogger(PubSubLookUpJob.class);

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        String projectId = "hl7-dev-274911";
        String subscriptionId = "pegah-results-sub";

        try {
            ProjectSubscriptionName subscriptionName =
                    ProjectSubscriptionName.of(projectId, subscriptionId);

            // Instantiate an asynchronous message receiver.
            MessageReceiver receiver =
                    (PubsubMessage message, AckReplyConsumer consumer) -> {
                        // Handle incoming message, then ack the received message.
                        System.out.println("Id: " + message.getMessageId());
                        System.out.println("Data: " + message.getData().toStringUtf8());
                        processCovidTestResults(message);
                        consumer.ack();
                    };

            Subscriber subscriber = null;
            ProjectSubscriptionName resultSubName = ProjectSubscriptionName.of(projectId, subscriptionId);
            ExecutorProvider resultsSubExecProvider = InstantiatingExecutorProvider.newBuilder().setExecutorThreadCount(10).build();
            subscriber = Subscriber.newBuilder(resultSubName, receiver)
                    .setParallelPullCount(10)
                    .setExecutorProvider(resultsSubExecProvider)
                    .setMaxAckExtensionPeriod(Duration.ofSeconds(120))
                    .build();
            try {
                subscriber.startAsync().awaitRunning(1L, TimeUnit.MINUTES);
                logger.info("Started subscription receiver for {}", subscriptionId);
            }
            catch (TimeoutException e) {
                throw new RuntimeException("Timed out while starting pubsub subscription " + subscriptionId, e);
            }
        }
        catch (Exception e) {
            throw new RuntimeException("Failed to get results from pubsub ", e);
        }

    }

    public static void processCovidTestResults(PubsubMessage message) {
        String data = message.getData().toStringUtf8();
        TestBostonResult testBostonResult = new Gson().fromJson(data, TestBostonResult.class);
        writeResultsIntoDB(testBostonResult);
        tellPepperAboutTheNewResults(testBostonResult);
    }

    private static void tellPepperAboutTheNewResults(TestBostonResult testBostonResult) {
        String query = "select " +
                "        eve.event_name, " +
                "        eve.event_type, " +
                "        request.ddp_participant_id, " +
                "        request.dsm_kit_request_id, " +
                "        realm.ddp_instance_id, " +
                "        realm.instance_name, " +
                "        realm.base_url, " +
                "        realm.auth0_token, " +
                "        realm.notification_recipients, " +
                "        realm.migrated_ddp, " +
                "        kit.receive_date, " +
                "        kit.scan_date " +
                "        from " +
                "        ddp_kit_request request, " +
                "        ddp_kit kit, " +
                "        event_type eve, " +
                "        ddp_instance realm " +
                "        where request.dsm_kit_request_id = kit.dsm_kit_request_id " +
                "        and request.ddp_instance_id = realm.ddp_instance_id " +
                "        and (eve.ddp_instance_id = request.ddp_instance_id " +
                "        and eve.kit_type_id = request.kit_type_id) " +
                "        and eve.event_type = \"RESULT\" " + // that's the change from the original query
                "        and request.external_order_number = ?"; // that's the change from the original query

        KitDDPNotification kitDDPNotification = KitDDPNotification.getKitDDPNotification(query, testBostonResult.getSampleId());
        if (kitDDPNotification != null) {
            EventUtil.triggerDDP(kitDDPNotification);
        }
    }

    public static void writeResultsIntoDB(TestBostonResult testBostonResult) {
        String query = "UPDATE ddp_kit SET  test_result = ?, result_date= ?  WHERE dsm_kit_id <> 0 and  dsm_kit_id  in ( select  dsm_kit_id from (select * from ddp_kit) as something where kit_label= ?  )";
        SimpleResult results = inTransaction((conn) -> {
            SimpleResult dbVals = new SimpleResult();
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                if (StringUtils.isNotBlank(testBostonResult.getResult())
                        && StringUtils.isNotBlank(testBostonResult.getTimeCompleted())
                        && StringUtils.isNotBlank(testBostonResult.getSampleId())) {
                    stmt.setString(1, testBostonResult.getResult());
                    stmt.setString(2, testBostonResult.getTimeCompleted());
                    stmt.setString(3, testBostonResult.getSampleId());
                    stmt.executeUpdate();
                }
            }
            catch (SQLException ex) {
                dbVals.resultException = ex;
            }
            return dbVals;
        });

        if (results.resultException != null) {
            throw new RuntimeException("Couldn't update the test results for kit label" + testBostonResult.getSampleId(), results.resultException);
        }

    }
}
