package org.broadinstitute.dsm.log;

import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;


import ch.qos.logback.classic.spi.LoggingEvent;
import com.google.gson.Gson;
import org.apache.log4j.Category;
import org.apache.log4j.Logger;
import org.apache.log4j.Priority;
import org.broadinstitute.dsm.TestHelper;
import org.junit.Before;
import org.junit.Test;
import org.mockserver.model.JsonBody;

import static org.junit.Assert.*;

public class SlackAppenderTest extends TestHelper {


    private LoggingEvent loggingEvent = new LoggingEvent();


    @Before
    public void setUp() {
        loggingEvent.setMessage("Hi there");
        loggingEvent.setCallerData(new StackTraceElement[]{new StackTraceElement(
                "Foo",
                "bar",
                "Foo.java",
                0
        )});
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

            slackAppender.start();
            slackAppender.doAppend(loggingEvent);

            slackAppender.waitForClearToQueue(3000);

            mockDDP.verify(request().withPath("/mock_slack_test").withBody(JsonBody.json(
                    new Gson().toJson(new SlackAppender.SlackMessagePayload("*Hi there*\n ``````",
                            "SlackChannel",
                            "Pepper",
                            ":nerd_face:")))));
        }


    }


}