package org.broadinstitute.dsm;

import org.broadinstitute.dsm.jobs.UPSTrackingJob;
import org.broadinstitute.dsm.model.ups.UPSTrackingResponse;
import org.junit.Test;

public class UPSTrackingJobTest {

    @Test
    public void testLookupTrackingInfo() {
        UPSTrackingResponse response = UPSTrackingJob.lookupTrackingInfo("92748902711160543400016310");
        System.out.println(response);
    }
}
