package org.broadinstitute.dsm.log;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.typesafe.config.Config;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.entity.ContentType;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.ThrowableInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class SlackAppender extends AppenderSkeleton {
    private static final Logger LOG = LoggerFactory.getLogger(SlackAppender.class);

    private URI slackHookUrl;

    private String channel;

    private static final String TITLE = "$TITLE$";

    private static final String STACK_TRACE = "$STACK_TRACE$";

    private static final String MESSAGE = "*" + TITLE  +  "*\n ```" + STACK_TRACE + "```";

    private boolean canLog = true;

    private List<SlackMessagePayload> messagesToSend = Collections.synchronizedList(new ArrayList<>());

    private int queueSize;

    private int intervalInMillis;

    private HttpClient httpClient;
    private ScheduledThreadPoolExecutor executorService;

    public SlackAppender() {
        Config cfg = ConfigManager.getInstance().getConfig();
        String slackHook = null;
        String slackChannel = null;
        Integer configQueueSize = null;
        Integer configIntervalInMillis = null;
        if (cfg != null) {
            if (cfg.hasPath(ConfigFile.SLACK_HOOK)) {
                slackHook = cfg.getString(ConfigFile.SLACK_HOOK);
            }
            if (cfg.hasPath(ConfigFile.SLACK_CHANNEL)) {
                slackChannel = cfg.getString(ConfigFile.SLACK_CHANNEL);
            }
            if (cfg.hasPath(ConfigFile.SLACK_QUEUE_SIZE)) {
                configQueueSize = cfg.getInt(ConfigFile.SLACK_QUEUE_SIZE);
            }
            if (cfg.hasPath(ConfigFile.SLACK_INTERVAL_IN_MILLIS)) {
                configIntervalInMillis = cfg.getInt(ConfigFile.SLACK_INTERVAL_IN_MILLIS);
            }
            init(slackHook, slackChannel, configQueueSize, configIntervalInMillis);
        }

    }

    public SlackAppender(String slackHookUrl, String slackChannel, int queueSize, int intervalInMillis) {
        init(slackHookUrl, slackChannel, queueSize, intervalInMillis);
    }

    private String getExceptionMessage(LoggingEvent e) {
        String exceptionMessage = "";
        if (e.getRenderedMessage() != null) {
            exceptionMessage = e.getRenderedMessage();
        }
        ThrowableInformation throwableInformation = e.getThrowableInformation();
        if (throwableInformation != null) {
            String causalMessage = "";
            if (throwableInformation.getThrowable().getCause() != null) {
                causalMessage = throwableInformation.getThrowable().getCause().getMessage();
            } else {
                causalMessage = throwableInformation.getThrowable().getMessage();
            }
            exceptionMessage += " " + causalMessage;
        }
        return exceptionMessage;
    }

    private String getStringifiedStackTrace(LoggingEvent e) {
        String stackTrace = "";
        ThrowableInformation throwableInformation = e.getThrowableInformation();
        if (throwableInformation != null) {
            if (throwableInformation.getThrowable().getCause() != null) {
                stackTrace = stringifyStackTrace(throwableInformation.getThrowable().getCause().getStackTrace());
            } else {
                stackTrace = stringifyStackTrace(throwableInformation.getThrowable().getStackTrace());
            }
        }
        return stackTrace;
    }

    private String stringifyStackTrace(StackTraceElement[] stackTrace) {
        StringBuilder stackTraceBuilder = new StringBuilder();
        for (StackTraceElement stackTraceEl: stackTrace) {
            stackTraceBuilder.append(stackTraceEl.toString()).append("\n");
        }
        return stackTraceBuilder.toString();
    }

    private void init(String slackHookUrl, String slackChannel, Integer queueSize, Integer intervalInMillis) {
        httpClient = HttpClient.newHttpClient();
        try {
            this.slackHookUrl = new URI(slackHookUrl);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Could not parse " + slackHookUrl);
        }

        this.channel = slackChannel;
        if (queueSize != null) {
            this.queueSize = queueSize;
        } else {
            this.queueSize = 10;
        }

        if (intervalInMillis != null) {
            this.intervalInMillis = intervalInMillis;
        } else {
            this.intervalInMillis = 60000;
        }

        if (StringUtils.isBlank(slackHookUrl)) {
            LOG.warn("No logs will go to slack.");
            canLog = false;
        }
        if (StringUtils.isBlank(slackChannel)) {
            LOG.warn("No logs will go to slack.");
            canLog = false;
        }

        if (canLog) {
            LOG.info("At most {} slack alerts will be sent to {} every {} ms", queueSize, slackChannel, intervalInMillis);
            executorService = new ScheduledThreadPoolExecutor(1,
                    runnable -> {
                        ThreadGroup threadGroup = new ThreadGroup("SlackAppender");
                        Thread t = new Thread(threadGroup, runnable);
                        t.setPriority(Thread.MIN_PRIORITY);
                        return t;
                    });

            executorService.scheduleWithFixedDelay(() -> {
                sendQueuedMessages();
            }, 0, this.intervalInMillis, TimeUnit.MILLISECONDS);
        } else {
            LOG.info("No slack alerts will be sent.");
        }
    }

    private boolean isQueueFull() {
        synchronized (messagesToSend) {
            return messagesToSend.size() >= queueSize;
        }
    }

    @Override
    protected void append(LoggingEvent e) {
        if (canLog) {
            synchronized (messagesToSend) {
                if (!isQueueFull()) {
                    String exceptionMessage = getExceptionMessage(e);
                    String stackTrace = getStringifiedStackTrace(e);

                    String message = MESSAGE.replace(TITLE, exceptionMessage);
                    message = message.replace(STACK_TRACE, stackTrace);

                    SlackMessagePayload messagePayload = new SlackMessagePayload(message, channel, "Pepper",
                            ":nerd_face:");
                    messagesToSend.add(messagePayload);
                }
            }
        }
    }

    private void sendQueuedMessages() {
        LOG.info("Sending {} messages to slack", messagesToSend.size());
        synchronized (messagesToSend) {
            for (SlackMessagePayload payload : messagesToSend) {
                sendMessage(payload);
            }
            messagesToSend.clear();
        }
    }

    private void sendMessage(SlackMessagePayload payload) {

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
            LOG.error("Could not post error message to slack room " + channel + " with hook " + slackHookUrl, e);
        }
    }

    @Override
    public void close() {

    }

    @Override
    public boolean requiresLayout() {
        return false;
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
