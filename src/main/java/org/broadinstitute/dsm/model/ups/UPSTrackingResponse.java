package org.broadinstitute.dsm.model.ups;

public class UPSTrackingResponse {
    UPSTrackResponse trackResponse;
    UPSError[] errors;
}
class UPSTrackResponse{
    UPSShipment[] shipment;
}
class UPSError{
    String code;
    String message;
}
