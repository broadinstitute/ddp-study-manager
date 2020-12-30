package org.broadinstitute.dsm.jobs;

import org.broadinstitute.dsm.DSMServer;
import org.broadinstitute.dsm.model.KitRequest;
import org.broadinstitute.dsm.model.KitType;
import org.broadinstitute.dsm.util.externalShipper.ExternalShipper;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class ExternalShipperJob implements Job {

    private static final Logger logger = LoggerFactory.getLogger(ExternalShipperJob.class);

    public void execute(JobExecutionContext context) {
        //get all kits requests settings with external shippers
        List<KitType> kitTypes = KitType.getKitTypesWithExternalShipper();
        for (KitType kitType : kitTypes) {
            try {
                logger.info("Starting the external shipper job");
                ExternalShipper shipper = (ExternalShipper) Class.forName(DSMServer.getClassName(kitType.getExternalShipper())).newInstance();//GBFRequestUtil
                shipper.updateOrderStatusForPendingKitRequests(kitType.getInstanceId());
                Instant now = Instant.now();
                for( int i = 5; i > 0; i--) {
                    Instant dynamicStartTime = now.minus(i, ChronoUnit.DAYS);
                    long end = 0L;
                    if (i != 1) {
                        Instant dynamicEndTime = now.minus(i - 1, ChronoUnit.DAYS);
                        end = dynamicEndTime.toEpochMilli();
                    }
                    else {
                        end = now.toEpochMilli();
                    }
                    shipper.orderConfirmation(dynamicStartTime.toEpochMilli(), end);
                }
//                Instant dynamicStartTime = now.minus(5, ChronoUnit.DAYS);
//                shipper.orderConfirmation(dynamicStartTime.toEpochMilli(), now.toEpochMilli());
            }
            catch (Exception e) {
                throw new RuntimeException("Failed to get status and/or confirmation ", e);
            }
        }
    }
}
