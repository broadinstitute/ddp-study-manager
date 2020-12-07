package org.broadinstitute.dsm.util.externalShipper;

import org.broadinstitute.dsm.model.KitRequest;
import org.broadinstitute.dsm.model.KitRequestSettings;
import org.broadinstitute.dsm.util.EasyPostUtil;

import java.util.ArrayList;

public interface ExternalShipper {

    public String getExternalShipperName();

    public void orderKitRequests(ArrayList<KitRequest> kitRequests, EasyPostUtil easyPostUtil, KitRequestSettings kitRequestSettings, String shippingCarrier) throws Exception;

    public void orderStatus(ArrayList<KitRequest> kitRequests) throws Exception;

    public void orderConfirmation(ArrayList<KitRequest> kitRequests, long startDate, long endDate) throws Exception;

    public void orderCancellation(ArrayList<KitRequest> kitRequests) throws Exception;

    public ArrayList<KitRequest> getKitRequestsNotDone(int instanceId);

}
