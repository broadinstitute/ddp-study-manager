package org.broadinstitute.dsm.jobs;

import org.broadinstitute.dsm.DSMServer;
import org.broadinstitute.dsm.model.KitRequest;
import org.broadinstitute.dsm.model.KitType;
import org.broadinstitute.dsm.statics.DBConstants;
import org.broadinstitute.dsm.util.DBUtil;
import org.broadinstitute.dsm.util.externalShipper.ExternalShipper;
import org.quartz.CronExpression;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
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
                ArrayList<KitRequest> kitRequests = shipper.getKitRequestsNotDone(kitType.getInstanceId());
                shipper.orderStatus(kitRequests);
                if (kitRequests != null && !kitRequests.isEmpty()) { // only if there are kits which are not yet having kit_label set
                    Instant now = Instant.now();
                    shipper.orderConfirmation(kitRequests, now.minus(30, ChronoUnit.DAYS).toEpochMilli(), now.toEpochMilli());
                }
            }
            catch (Exception e) {
                throw new RuntimeException("Failed to get status and/or confirmation ", e);
            }
        }
    }
}
