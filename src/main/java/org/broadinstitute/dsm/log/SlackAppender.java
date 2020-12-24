package org.broadinstitute.dsm.log;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class SlackAppender {
    private static final Logger LOG = LoggerFactory.getLogger(SlackAppender.class);

    private HttpClient httpClient;
    private ScheduledThreadPoolExecutor executorService;
    private URI slackHookUrl;
    private String channel;
    private int queueSize;
    private int intervalInMillis;
    private boolean canLog = true;

    public SlackAppender(String slackHookUrl, String slackChannel, int queueSize, int intervalInMillis) {
        init(slackHookUrl, slackChannel, queueSize, intervalInMillis);
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
                    new ThreadFactory("SlackAppender", Thread.MIN_PRIORITY));
            executorService.scheduleWithFixedDelay(() -> {
                sendQueuedMessages();
            }, 0, this.intervalInMillis, TimeUnit.MILLISECONDS);
        } else {
            LOG.info("No slack alerts will be sent.");
        }
    }
}
