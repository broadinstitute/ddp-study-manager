package org.broadinstitute.dsm.route;

import org.broadinstitute.ddp.handlers.util.Result;
import org.broadinstitute.dsm.db.Drug;
import org.broadinstitute.dsm.statics.UserErrorMessages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;
import spark.Route;

import java.util.List;

public class DrugEntryRoute implements Route {

    private static final Logger logger = LoggerFactory.getLogger(DrugEntryRoute.class);

    @Override
    public Object handle(Request request, Response response) throws Exception {
        try {
            List<Drug> drugList = Drug.getFullDrugData();
            return drugList;
        }
        catch(Exception e) {
            logger.error("Attempt to get drug data gave an error: " , e);
            return new Result(500, UserErrorMessages.CONTACT_DEVELOPER);
        }
    }
}
