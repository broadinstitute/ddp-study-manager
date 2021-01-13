package org.broadinstitute.dsm.log;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.http.HttpHeaders;
import org.apache.http.entity.ContentType;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.LoggingEvent;
import org.broadinstitute.ddp.util.Utility;
import org.broadinstitute.dsm.DSMServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SlackAppender extends AppenderSkeleton {
    public SlackAppender() {
    }

    private static final Logger logger = LoggerFactory.getLogger(SlackAppender.class);
    public static final String APPENDER_NAME = "slackAppender";
    private static HttpClient httpClient;
    private static String appEnv;
    private static String schedulerName;
    private static URI slackHookUrl;
    private static String slackChannel;
    private static boolean configured = false;
    private static AtomicLong minEpochForNextJobError = new AtomicLong(0L);
    private static AtomicLong minEpochForNextError = new AtomicLong(0L);
    private static final int JOB_DELAY = 60;
    private static final int NON_JOB_DELAY = 30;
    private static final String MSG_TYPE_JOB_ERROR = "JOB_ERROR_ALERT";
    private static final String MSG_TYPE_ERROR = "ERROR_ALERT";

    @Override
    protected void append(LoggingEvent event) {
        if (configured && event.getLevel().toInt() == 40000) {
            try {
                boolean jobError = schedulerName != null && event.getThreadName().contains(schedulerName);
                long currentEpoch = Utility.getCurrentEpoch();
                if (jobError && currentEpoch >= minEpochForNextJobError.get()) {
                    this.sendSlackNotification(currentEpoch, String.format("This looks like a job error. Job error reporting is " +
                            "throttled so you will only see 1 per %s minutes.", JOB_DELAY), MSG_TYPE_JOB_ERROR);
                    minEpochForNextJobError.set(currentEpoch + 3600L);
                } else if (!jobError && currentEpoch >= minEpochForNextError.get()) {
                    this.sendSlackNotification(currentEpoch, String.format("This does NOT look like a job error. " +
                            "Non-job error reporting is throttled so you will only see 1 per %s minutes.", NON_JOB_DELAY), MSG_TYPE_ERROR);
                    minEpochForNextError.set(currentEpoch + 1800L);
                }
            } catch (Exception var5) {
                logger.warn("ErrorNotificationAppender Error: " + ExceptionUtils.getStackTrace(var5));
            }
        }
    }

    private void sendSlackNotification(long currentEpoch, String note, String messageType) {
        SlackMessagePayload payload = getSlackMessagePayload(currentEpoch, note, messageType);
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(slackHookUrl)
                .POST(HttpRequest.BodyPublishers.ofString(new Gson().toJson(payload)))
                .header(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
                .build();

        try {
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IOException("Could not post " + payload + " to slack.  Hook returned " + response.body());
            }
        } catch (IOException | InterruptedException e) {
            logger.warn("Could not post error message to slack room " + slackChannel + " with hook " + slackHookUrl
                    + "\n" + ExceptionUtils.getStackTrace(e));
        }
    }

    public SlackMessagePayload getSlackMessagePayload(long currentEpoch, String note, String messageType) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String date = formatter.format(new Date(currentEpoch * 1000L));
        SlackMessagePayload payload = new SlackMessagePayload(String.format("An error has been detected for *%s*. Please go check the backend logs around *%s UTC*. \n" +
                "%s \n %s", appEnv, date, messageType, note), slackChannel, "DSM", ":nerd_face:");
        return payload;
    }

    @Override
    public void close() {

    }

    @Override
    public boolean requiresLayout() {
        return false;
    }


    public static synchronized void configure(String scheduler, String appEnv, URI slackHookUri, String slackChannel) {
        if (!configured) {
            SlackAppender.appEnv = appEnv;
            slackHookUrl = slackHookUri;
            httpClient = HttpClient.newHttpClient();
            SlackAppender.slackChannel = slackChannel;
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
