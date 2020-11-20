package org.broadinstitute.dsm.model.ups;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;

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
        Date theDate = null;
        if (date != null) {
            try {
                theDate = new SimpleDateFormat("yyyyMMdd kkmmss").parse(getDateTimeString());
            } catch (ParseException e) {
                throw new RuntimeException("Could parse date " + date + " to a proper date object", e);
            }
        }
        return theDate.toInstant();
    }

    public boolean isInTransit() {
        return status.isInTransit();
    }

    public boolean isDelivered() {
        return status.isDelivered();
    }
}
