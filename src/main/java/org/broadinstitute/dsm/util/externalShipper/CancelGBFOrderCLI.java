package org.broadinstitute.dsm.util.externalShipper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.broadinstitute.dsm.model.gbf.CancelResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CancelGBFOrderCLI {

    private static final Logger logger = LoggerFactory.getLogger(CancelGBFOrderCLI.class);


    public static void main(String[] args) {
        String cancelUrl = args[0];
        String apiKey = args[1];
        String ordersFile = args[2];
        FileReader ordersReader = null;
        List<String> ordersToCancel = new ArrayList<>();

        try {
            ordersReader = new FileReader(new File(ordersFile));
            ordersToCancel = IOUtils.readLines(ordersReader);
        } catch(IOException e) {
            logger.error("Could read orders from file " + ordersFile,e);
        }

        if (ordersReader != null) {
            CancelResponse cancelResponse = null;
            for (String orderNumber : ordersToCancel) {
                try {
                    cancelResponse = GBFRequestUtil.cancelOrder(orderNumber, cancelUrl, apiKey);
                    if (cancelResponse.wasSuccessful()) {
                        logger.info("Cancelled order {}", orderNumber);
                    } else {
                        logger.error("Could not cancel order {} due to {}",orderNumber, cancelResponse.getErrorMessage());
                    }
                } catch(Exception e) {
                    logger.error("Could not cancel order {}", orderNumber, e);
                }
            }
        }


    }
}
