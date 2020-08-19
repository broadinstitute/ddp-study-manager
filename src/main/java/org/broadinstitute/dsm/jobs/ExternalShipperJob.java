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
                if (kitRequests != null && !kitRequests.isEmpty()) { // only if there are kits which are not yet shipped out
                    JobDataMap dataMap = context.getJobDetail().getJobDataMap();
                    String additionalCronExpression = (String) dataMap.get(DSMServer.ADDITIONAL_CRON_EXPRESSION);
                    if (CronExpression.isValidExpression(additionalCronExpression)) {
                        CronExpression cron = new CronExpression(additionalCronExpression);
                        if (cron.isSatisfiedBy(context.getFireTime())) {
                            long lastRun = DBUtil.getBookmark(DBConstants.GBF_CONFIRMATION);
                            long now = System.currentTimeMillis();
                            shipper.orderConfirmation(kitRequests, lastRun, now);
                            DBUtil.updateBookmark(now, DBConstants.GBF_CONFIRMATION);
                        }
                    }
                    else {
                        throw new RuntimeException("Additional cron expression is not a valid cron expression");
                    }
                }
            }
            catch (Exception e) {
                throw new RuntimeException("Failed to get status and/or confirmation ", e);
            }
        }
    }
}
