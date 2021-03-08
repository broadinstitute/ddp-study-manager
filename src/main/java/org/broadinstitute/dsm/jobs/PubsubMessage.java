package org.broadinstitute.dsm.jobs;

import java.util.Map;

public class PubsubMessage {
        String data;
        Map<String, String> attributes;
        String messageId;
        String publishTime;
    }
