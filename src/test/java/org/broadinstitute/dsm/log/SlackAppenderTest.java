package org.broadinstitute.dsm.log;

import org.junit.Test;
import org.mockserver.client.server.MockServerClient;

import static org.junit.Assert.*;

public class SlackAppenderTest {

    private MockServerClient mockServerClient;

    @Test
    public void testSuccessfulLoggingToSlack() {
        mockServerClient = mockServerClient.reset();


    }


}