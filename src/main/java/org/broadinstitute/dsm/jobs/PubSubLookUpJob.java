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
import org.broadinstitute.dsm.model.birch.TestBostonResult;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.threeten.bp.Duration;

import java.sql.PreparedStatement;
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

        try{
            ProjectSubscriptionName subscriptionName =
                    ProjectSubscriptionName.of(projectId, subscriptionId);

            // Instantiate an asynchronous message receiver.
            MessageReceiver receiver =
                    (PubsubMessage message, AckReplyConsumer consumer) -> {
                        // Handle incoming message, then ack the received message.
                        System.out.println("Id: " + message.getMessageId());
                        System.out.println("Data: " + message.getData().toStringUtf8());
                        writeResultsIntoDB(message);
                        consumer.ack();
                    };

            Subscriber subscriber = null;
            ProjectSubscriptionName resultSubName = ProjectSubscriptionName.of(projectId, subscriptionId);
            ExecutorProvider resultsSubExecProvider = InstantiatingExecutorProvider.newBuilder().setExecutorThreadCount(10).build();
             subscriber = Subscriber.newBuilder(resultSubName, receiver)
                    .setParallelPullCount(10)
                    .setExecutorProvider(resultsSubExecProvider)
                    .setMaxAckExtensionPeriod(Duration.ofSeconds(30))
                    .build();
            try {
                subscriber.startAsync().awaitRunning(1L, TimeUnit.MINUTES);
                logger.info("Started subscription receiver for {}", subscriptionId);
            } catch (TimeoutException e) {
                throw new RuntimeException("Timed out while starting pubsub subscription " + subscriptionId, e);
            }
        }
        catch (Exception e){
            throw new RuntimeException("Failed to get results from pubsub ", e);
        }

    }

    public static void writeResultsIntoDB(PubsubMessage message){
        String data = message.getData().toStringUtf8();
        TestBostonResult testBostonResult = new Gson().fromJson(data, TestBostonResult.class);
        String query = "UPDATE ddp_kit SET  test_result = ?, result_date= ?  WHERE dsm_kit_id <> 0 and  dsm_kit_id  in ( select  dsm_kit_id from (select * from ddp_kit) as something where kit_label= ?  )";
        TestBostonResult finalTestBostonResult = testBostonResult;
        SimpleResult results = inTransaction((conn) -> {
            SimpleResult dbVals = new SimpleResult();
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                if (StringUtils.isNotBlank(finalTestBostonResult.result) && StringUtils.isNotBlank(finalTestBostonResult.date) && StringUtils.isNotBlank(finalTestBostonResult.sampleId)) {
                    stmt.setString(1, finalTestBostonResult.result);
                    stmt.setString(2, finalTestBostonResult.date);
                    stmt.setString(3, finalTestBostonResult.sampleId);
                    stmt.executeUpdate();
                }
            }
            catch (SQLException ex) {
                dbVals.resultException = ex;
            }
            return dbVals;
        });

        if (results.resultException != null) {
            throw new RuntimeException("Couldn't update the test results for kit label" + testBostonResult.sampleId, results.resultException);
        }

    }
}
