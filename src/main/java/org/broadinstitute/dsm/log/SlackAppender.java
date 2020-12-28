package org.broadinstitute.dsm.log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.typesafe.config.Config;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.LoggingEvent;
import org.broadinstitute.ddp.email.SendGridClient;
import org.broadinstitute.ddp.util.EDCClient;
import org.broadinstitute.ddp.util.Utility;

public class SlackAppender extends AppenderSkeleton {

    public static final String APPENDER_NAME = "slackAppender";
    private static SendGridClient emailClient;
    private static String appEnv;
    private static String schedulerName;
    private static String slackHook;
    private static String slackChannel;
    private static boolean configured = false;
    private static AtomicLong minEpochForNextJobError = new AtomicLong(0L);
    private static AtomicLong minEpochForNextError = new AtomicLong(0L);
    private static final int JOB_DELAY = 60;
    private static final int NON_JOB_DELAY = 30;
    private static final String ERROR_MESSAGE = "<p>An error has been detected for <b>%s</b>. Please go check the backend logs around <b>%s UTC</b>.</p><p>%s</p><p>%s</p>";
    private static final String JOB_THROTTLE_NOTE = "This looks like a job error. Job error reporting is throttled so you will only see 1 per %s minutes.";
    private static final String NON_JOB_THROTTLE_NOTE = "This does NOT look like a job error. Non-job error reporting is throttled so you will only see 1 per %s minutes.";
    private static final String MSG_TYPE_JOB_ERROR = "JOB_ERROR_ALERT";
    private static final String MSG_TYPE_ERROR = "ERROR_ALERT";

    @Override
    protected void append(LoggingEvent event) {
        if (configured && event.getLevel().toInt() == 40000) {
            try {
                boolean jobError = schedulerName != null && event.getThreadName().contains(schedulerName);
                long currentEpoch = Utility.getCurrentEpoch();
                if (jobError && currentEpoch >= minEpochForNextJobError.get()) {
                    this.sendSlackNotification(currentEpoch, String.format("This looks like a job error. Job error reporting is throttled so you will only see 1 per %s minutes.", 60), "JOB_ERROR_ALERT");
                    minEpochForNextJobError.set(currentEpoch + 3600L);
                } else if (!jobError && currentEpoch >= minEpochForNextError.get()) {
                    this.sendSlackNotification(currentEpoch, String.format("This does NOT look like a job error. Non-job error reporting is throttled so you will only see 1 per %s minutes.", 30), "ERROR_ALERT");
                    minEpochForNextError.set(currentEpoch + 1800L);
                }
            } catch (Exception var5) {
                System.out.println("ErrorNotificationAppender Error: " + ExceptionUtils.getStackTrace(var5));
            }
        }
    }

    private void sendSlackNotification(long currentEpoch, String note, String messageType) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String date = formatter.format(new Date(currentEpoch * 1000L));

    }

    @Override
    public void close() {

    }

    @Override
    public boolean requiresLayout() {
        return false;
    }


    public static synchronized void configure(Config config, String scheduler) {
        if (config == null) {
            throw new NullPointerException("config");
        } else if (!configured) {
            appEnv = config.getString("portal.environment");
            slackHook = config.getString("slack.hook");
            slackChannel = config.getString("slack.channel");
            emailClient = new SendGridClient();
            JsonObject settings = (JsonObject)((JsonObject)(new JsonParser()).parse(config.getString("errorAlert.clientSettings")));
            emailClient.configure(config.getString("errorAlert.key"), settings, "", (EDCClient)null, appEnv);
            schedulerName = scheduler;
            configured = true;
        } else {
            throw new RuntimeException("Configure has already been called for this appender.");
        }
    }
}
