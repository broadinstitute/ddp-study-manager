package org.broadinstitute.dsm.model.ups;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;

import lombok.Data;

@Data
public class UPSActivity {
    UPSLocation location;
    UPSStatus status;
    String date;
    String time;

    public UPSActivity() {}

    public UPSActivity(UPSStatus status, String date, String time) {
        this.status = status;
        this.date = date;
        this.time = time;
    }

    public String getDateTimeString() {
        return date + " " + time;
    }

    public Instant getInstant() {
        Instant eventTime = null;
        String dateTime = getDateTimeString();
        if (dateTime != null) {
            try {
                eventTime = new SimpleDateFormat("yyyyMMdd kkmmss").parse(dateTime).toInstant();
            } catch (ParseException e) {
                throw new RuntimeException("Could parse date " + date + " to a proper date object", e);
            }
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
