package org.broadinstitute.dsm.log;

import static org.junit.Assert.*;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import com.google.gson.Gson;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigValueFactory;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.RootLogger;
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

    private LoggingEvent loggingEvent = new LoggingEvent(null, new RootLogger(Level.ERROR), Level.ERROR, null, null);

    public Config cfg;


    @Before
    public void setUp() {
        loggingEvent.setProperty("Hi", "There");
        setupDB();
        cfg = TestHelper.cfg;
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

            cfg = cfg.withValue("slack.hook", ConfigValueFactory.fromAnyRef("http://localhost:" + mockDDP.getPort() + "/mock_slack_test"));
            cfg = cfg.withValue("slack.channel", ConfigValueFactory.fromAnyRef("SlackChannel"));

            SlackAppender.configure(cfg, null);

            slackAppender.doAppend(loggingEvent);

            mockDDP.verify(request().withPath("/mock_slack_test").withBody(JsonBody.json(
                    new Gson().toJson(new SlackAppender.SlackMessagePayload("*Hi there*\n ``````",
                            "SlackChannel",
                            "DSM",
                            ":nerd_face:")))));
        } else {
            Assert.fail("Mock slack not running");
        }
    }

}