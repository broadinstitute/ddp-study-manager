package org.broadinstitute.dsm.log;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import ch.qos.logback.core.AppenderBase;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.entity.ContentType;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.ThrowableInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
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

public class SlackAppender extends AppenderBase<ILoggingEvent> {
    private static final Logger LOG = LoggerFactory.getLogger(SlackAppender.class);
    private static final String VAULT_DOT_CONF = "vault.conf";
    public static final String SLACK_HOOK = "slack.hook";
    public static final String SLACK_CHANNEL = "slack.channel";
    public static final String SLACK_QUEUE_SIZE = "slack.queueSize";
    public static final String SLACK_INTERVAL_IN_MILLIS = "slack.intervalInMillis";

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
        Config cfg = ConfigFactory.load();
        File vaultConfig = new File(VAULT_DOT_CONF);
        cfg = cfg.withFallback(ConfigFactory.parseFile(vaultConfig));
        String slackHook = null;
        String slackChannel = null;
        Integer configQueueSize = null;
        Integer configIntervalInMillis = null;
        if (cfg != null) {
            if (cfg.hasPath(SLACK_HOOK)) {
                slackHook = cfg.getString(SLACK_HOOK);
            }
            if (cfg.hasPath(SLACK_CHANNEL)) {
                slackChannel = cfg.getString(SLACK_CHANNEL);
            }
            if (cfg.hasPath(SLACK_QUEUE_SIZE)) {
                configQueueSize = cfg.getInt(SLACK_QUEUE_SIZE);
            }
            if (cfg.hasPath(SLACK_INTERVAL_IN_MILLIS)) {
                configIntervalInMillis = cfg.getInt(SLACK_INTERVAL_IN_MILLIS);
            }
            init(slackHook, slackChannel, configQueueSize, configIntervalInMillis);
        }

    }

    public SlackAppender(String slackHookUrl, String slackChannel, int queueSize, int intervalInMillis) {
        init(slackHookUrl, slackChannel, queueSize, intervalInMillis);
    }

    private String getExceptionMessage(ILoggingEvent e) {
        String exceptionMessage = "";
        if (e.getFormattedMessage() != null) {
            exceptionMessage = e.getFormattedMessage();
        }
        IThrowableProxy throwableProxy = e.getThrowableProxy();
        if (throwableProxy != null) {
            String causalMessage = "";
            if (throwableProxy.getCause() != null) {
                causalMessage = throwableProxy.getCause().getMessage();
            } else {
                causalMessage = throwableProxy.getMessage();
            }
            exceptionMessage += " " + causalMessage;
        }
        return exceptionMessage;
    }

    private String getStringifiedStackTrace(ILoggingEvent e) {
        String stackTrace = "";
        IThrowableProxy throwableProxy = e.getThrowableProxy();
        if (throwableProxy != null) {
            if (throwableProxy.getCause() != null) {
                stackTrace = stringifyStackTrace(throwableProxy.getCause().getStackTraceElementProxyArray());
            } else {
                stackTrace = stringifyStackTrace(throwableProxy.getStackTraceElementProxyArray());
            }
        }
        return stackTrace;
    }

    private String stringifyStackTrace(StackTraceElementProxy[] stackTrace) {
        StringBuilder stackTraceBuilder = new StringBuilder();
        for (StackTraceElementProxy stackTraceElt : stackTrace) {
            stackTraceBuilder.append(stackTraceElt.toString()).append("\n");
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
    protected void append(ILoggingEvent e) {
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

    public synchronized void waitForClearToQueue(long timeoutMillis) {
        if (!messagesToSend.isEmpty()) {
            try {
                wait(timeoutMillis);
            } catch (InterruptedException e) {
                LOG.error("Interrupted while waiting for queue to clear", e);
            }
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
