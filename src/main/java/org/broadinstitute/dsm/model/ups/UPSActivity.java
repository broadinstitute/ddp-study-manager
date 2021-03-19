package org.broadinstitute.dsm.model.ups;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import lombok.Data;

@Data
public class UPSActivity {
    UPSLocation location;
    UPSStatus status;
    String date;
    String time;
    String activityId;
    String packageId;
    String dsmKitRequestId;
    String locationString;


    public UPSActivity(UPSLocation location, UPSStatus status, String date, String time, String activityId, String packageId, String dsmKitRequestId) {
        this.location = location;
        this.status = status;
        this.date = date;
        this.time = time;
        this.activityId = activityId;
        this.packageId = packageId;
        this.dsmKitRequestId = dsmKitRequestId;
    }

    public UPSActivity(String locationString, UPSStatus status, String date, String time, String activityId, String packageId, String dsmKitRequestId) {
        this.locationString = locationString;
        this.status = status;
        this.date = date;
        this.time = time;
        this.activityId = activityId;
        this.packageId = packageId;
        this.dsmKitRequestId = dsmKitRequestId;
    }

    public String getDateTimeString() {
        return date + " " + time;
    }

    /**
     * Returns the instant of this event.  Assumes New York
     * time zone!
     */
    public Instant getInstant() {
        Instant eventTime = null;
        String dateTime = getDateTimeString();
        if (dateTime != null) {
            eventTime = DateTimeFormatter.ofPattern("yyyyMMdd HHmmss").withZone(ZoneId.of("America/New_York")).parse(dateTime, Instant::from);
        }
        return eventTime;
    }

    /**
     * Convenience method for {@link UPSStatus#isOnItsWay()}
     */
    public boolean isOnItsWay() {
        return status.isOnItsWay();
    }

    /**
     * Convenience method for {@link UPSStatus#isPickup()}
     */
    public boolean isPickup() {
        return status.isPickup();
    }
}
