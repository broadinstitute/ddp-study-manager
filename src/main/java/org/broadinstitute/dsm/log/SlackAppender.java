package org.broadinstitute.dsm.log;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpRequest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.typesafe.config.Config;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.entity.ContentType;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.LoggingEvent;
import org.broadinstitute.ddp.email.SendGridClient;
import org.broadinstitute.ddp.util.Utility;

public class SlackAppender extends AppenderSkeleton {

    public static final String APPENDER_NAME = "slackAppender";
    private static SendGridClient emailClient;
    private static String appEnv;
    private static String schedulerName;
    private static URI slackHookUrl;
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
            SlackMessagePayload payload = null;
            try {
                boolean jobError = schedulerName != null && event.getThreadName().contains(schedulerName);
                long currentEpoch = Utility.getCurrentEpoch();
                if (jobError && currentEpoch >= minEpochForNextJobError.get()) {
                    payload = new SlackMessagePayload(String.format("This looks like a job error. Job error reporting is " +
                            "throttled so you will only see 1 per %s minutes.", 60), slackChannel, "DSM", ":nerd_face:");
                    this.sendSlackNotification(currentEpoch, String.format("This looks like a job error. Job error reporting is " +
                            "throttled so you will only see 1 per %s minutes.", 60), "JOB_ERROR_ALERT", payload);
                    minEpochForNextJobError.set(currentEpoch + 3600L);
                } else if (!jobError && currentEpoch >= minEpochForNextError.get()) {
                    this.sendSlackNotification(currentEpoch, String.format("This does NOT look like a job error. Non-job error reporting is throttled so you will only see 1 per %s minutes.", 30), "ERROR_ALERT", );
                    minEpochForNextError.set(currentEpoch + 1800L);
                }
            } catch (Exception var5) {
                System.out.println("ErrorNotificationAppender Error: " + ExceptionUtils.getStackTrace(var5));
            }
        }
    }

    private void sendSlackNotification(long currentEpoch, String note, String messageType, SlackMessagePayload payload) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String date = formatter.format(new Date(currentEpoch * 1000L));
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(slackHookUrl)
                .POST(HttpRequest.BodyPublishers.ofString(new Gson().toJson(payload)))
                .header(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
                .build();
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
            try {
                slackHookUrl = new URI(config.getString("slack.hook"));
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException("Could not parse " + slackHookUrl);
            }
            slackChannel = config.getString("slack.channel");
            schedulerName = scheduler;
            configured = true;
        } else {
            throw new RuntimeException("Configure has already been called for this appender.");
        }
    }

    static class SlackMessagePayload {

        @SerializedName("text")
        private String text;

        @SerializedName("channel")
        private String channel;

        @SerializedName("username")
        private String username;

        @SerializedName("icon_emoji")
        private String iconEmoji;

        @SerializedName("unfurl_links")
        private boolean unfurlLinks = false;

        public SlackMessagePayload(String text, String channel, String username, String iconEmoji) {
            this.text = text;
            this.channel = channel;
            this.username = username;
            this.iconEmoji = iconEmoji;
        }
    }
}
