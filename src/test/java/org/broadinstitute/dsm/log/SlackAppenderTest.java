package org.broadinstitute.dsm.log;

import static org.junit.Assert.*;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import com.google.gson.Gson;
import org.apache.log4j.spi.LoggingEvent;
import org.broadinstitute.dsm.TestHelper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockserver.junit.MockServerRule;
import org.mockserver.model.JsonBody;

public class SlackAppenderTest extends TestHelper {


    @Rule
    public MockServerRule mockServerRule = new MockServerRule(this);


    private LoggingEvent loggingEvent = new LoggingEvent(null, null, null, null, null);

    @Before
    public void setUp() {
        loggingEvent.setProperty("Hi", "There");
    }

    @Test
    public void testSuccessfulLoggingToSlack() throws Exception {

        TestHelper.startMockServer();

        if (mockDDP.isRunning()) {
            mockDDP.when(request().withPath("/mock_slack_test"))
                    .respond(response()
                            .withStatusCode(200)
                            .withBody("ok"));

            SlackAppender slackAppender = new SlackAppender();

            slackAppender.;
            slackAppender.doAppend(loggingEvent);

            slackAppender.waitForClearToQueue(3000);

            mockServerClient.verify(request().withPath("/mock_slack_test").withBody(JsonBody.json(
                    new Gson().toJson(new SlackAppender.SlackMessagePayload("*Hi there*\n ``````",
                            "SlackChannel",
                            "Pepper",
                            ":nerd_face:")))));
        } else {
            Assert.fail("Mock slack not running");
        }
    }

}