package org.broadinstitute.dsm.log;

import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;


import org.apache.log4j.Category;
import org.apache.log4j.Logger;
import org.apache.log4j.Priority;
import org.apache.log4j.spi.LoggingEvent;
import org.broadinstitute.dsm.TestHelper;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class SlackAppenderTest extends TestHelper {

    private LoggingEvent loggingEvent = new LoggingEvent("", new Logger("bla"), Priority.ERROR, "bla", new Throwable());

    @Before
    public void setUp() {

    }

    @Test
    public void testSuccessfulLoggingToSlack() {
        TestHelper.startMockServer();

        if (mockDDP.isRunning()) {
            mockDDP.when(request().withPath("/mock_slack_test"))
                    .respond(response()
                            .withStatusCode(200)
                            .withBody("ok"));

            SlackAppender slackAppender = new SlackAppender("http://localhost:" + mockDDP.getPort() + "/mock_slack_test",
                    "SlackChannel", 100, 10);

            slackAppender.append();
        }


    }


}