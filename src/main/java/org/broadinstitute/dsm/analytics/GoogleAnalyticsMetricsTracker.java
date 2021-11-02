package org.broadinstitute.dsm.analytics;

import com.brsanthu.googleanalytics.GoogleAnalytics;
import com.brsanthu.googleanalytics.GoogleAnalyticsConfig;
import com.brsanthu.googleanalytics.request.EventHit;
import com.brsanthu.googleanalytics.request.GoogleAnalyticsResponse;
import com.typesafe.config.Config;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GoogleAnalyticsMetricsTracker {
    private static final Logger logger = LoggerFactory.getLogger(GoogleAnalyticsMetricsTracker.class);
    private static final Integer DEFAULT_BATCH_SIZE = 10;
    private static final String GA_TOKEN_PATH = "GoogleAnalytics.trackingId";//todo pegah add to SM
    private static GoogleAnalytics googleAnalyticsTrackers;
    private static volatile GoogleAnalyticsMetricsTracker instance;
    private static Object lockGA = new Object();
    private static Config CONFIG;

    private GoogleAnalyticsMetricsTracker() {
        initStudyMetricTracker();
    }

    public static void setConfig(@NonNull Config config){
        CONFIG = config;
    }

    public static GoogleAnalyticsMetricsTracker getInstance() {
        if (instance == null) {
            synchronized (lockGA) {
                if (instance == null) {
                    instance = new GoogleAnalyticsMetricsTracker();
                }
            }
        }
        return instance;
    }

    private GoogleAnalytics getMetricTracker() {
        if (googleAnalyticsTrackers == null) {

            initStudyMetricTracker();
        }
        return googleAnalyticsTrackers;
    }

    private synchronized void initStudyMetricTracker() {
        GoogleAnalytics metricTracker = GoogleAnalytics.builder()
                .withConfig(new GoogleAnalyticsConfig().setBatchingEnabled(true).setBatchSize(DEFAULT_BATCH_SIZE).setGatherStats(true))
                .withTrackingId(getAnalyticsToken(CONFIG))
                .build();
        googleAnalyticsTrackers = metricTracker;
        logger.info("Initialized GA Metrics Tracker for DSM ");
    }


    private void sendEventMetrics(EventHit eventHit) {
        GoogleAnalytics metricTracker = getMetricTracker();
        if (metricTracker != null) {
            GoogleAnalyticsResponse response = metricTracker.event().eventCategory(eventHit.eventCategory())
                    .eventAction(eventHit.eventAction())
                    .eventLabel(eventHit.eventLabel())
                    .eventValue(eventHit.eventValue())
                    .send();
            logger.info(response.getStatusCode()+"");
            logger.info(metricTracker.getStats().getEventHits()+"");

        }
    }

    public void sendAnalyticsMetrics(String studyGuid, String category, String action, String label,
                                     String labelContent, int value) {
        String gaEventLabel = String.join(":", label,
                studyGuid);
        if (labelContent != null) {
            gaEventLabel = String.join(":", gaEventLabel, labelContent);
        }
        EventHit eventHit = new EventHit(category, action, gaEventLabel, value);
        sendEventMetrics(eventHit);

    }

    public void flushOutMetrics() {
        //lookup all Metrics Trackers and flush out any pending events
        logger.info("Flushing out all pending GA events");
        googleAnalyticsTrackers.flush();
        googleAnalyticsTrackers.resetStats();
    }

    public String getAnalyticsToken(@NonNull Config config) {
        if (config.hasPath(GA_TOKEN_PATH)) {
            return config.getString(GA_TOKEN_PATH);
        }
        throw new RuntimeException("There is no secret in the SM for the Google Analytics");
    }


}
