package org.broadinstitute.dsm.functions.cloud;

import static org.junit.Assert.*;

import org.broadinstitute.dsm.TestHelper;
import org.broadinstitute.dsm.jobs.TestBostonUPSTrackingJob;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestBostonUPSTrackingCloudFunctionTest {

    private TestBostonUPSTrackingCloudFunction testBostonUPSTrackingCloudFunction;

    private static final Logger logger = LoggerFactory.getLogger(TestBostonUPSTrackingJob.class);


    @Before
    public void setUp() {
        testBostonUPSTrackingCloudFunction = new TestBostonUPSTrackingCloudFunction();
        TestHelper.setupDB();
    }

    @Test
    public void helloPubSub_shouldPrintName() {
        TestBostonUPSTrackingCloudFunction.PubSubMessage pubSubMessage = new TestBostonUPSTrackingCloudFunction.PubSubMessage();

        try {
            testBostonUPSTrackingCloudFunction.accept(pubSubMessage, null);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


}