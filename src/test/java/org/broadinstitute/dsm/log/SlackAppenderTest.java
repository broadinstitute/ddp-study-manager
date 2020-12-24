package org.broadinstitute.dsm.log;

import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;


import org.broadinstitute.dsm.TestHelper;
import org.junit.Before;
import org.junit.Test;
import org.mockserver.client.server.MockServerClient;
import org.mockserver.integration.ClientAndServer;

import static org.junit.Assert.*;

public class SlackAppenderTest extends TestHelper {

    @Test
    public void testSuccessfulLoggingToSlack() {
        TestHelper.startMockServer();

        if (mockDDP.isRunning()) {
            mockDDP.when(request().withPath("/mock_slack_test"))
                    .respond(response()
                            .withStatusCode(200)
                            .withBody("ok"));
        }

    }


}