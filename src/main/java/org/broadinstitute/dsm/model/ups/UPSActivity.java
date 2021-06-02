package org.broadinstitute.dsm.model.ups;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.dsm.db.dto.ups.UPSActivityDto;
import org.broadinstitute.dsm.db.dto.ups.UPShipmentDto;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
public class UPSActivity {

    private static final String ADDRESS = "address";

    UPSLocation location;
    UPSStatus status;
    String date;
    String time;
    String dateTime;
    String activityId;
    String packageId;
    String locationString;

    public UPSActivity() {}

    public UPSActivity(String locationString, UPSStatus status, String date, String time, String activityId,
                       String packageId, String dateTime) {
        this.locationString = locationString;
        this.status = status;
        this.date = date;
        this.time = time;
        this.activityId = activityId;
        this.packageId = packageId;
        this.dateTime = dateTime;
    }

    public UPSActivity(UPSLocation location, UPSStatus status, String date, String time, String activityId,
                       String packageId, String dateTime) {
        this.location = location;
        this.status = status;
        this.date = date;
        this.time = time;
        this.activityId = activityId;
        this.packageId = packageId;
        this.dateTime = dateTime;
    }

    public String getDateTimeString() {
        if (StringUtils.isBlank(this.getTime()) && StringUtils.isBlank(this.getDate())) {
            return null;
        }
        if (StringUtils.isBlank(this.getTime())) {
            this.setTime("000000");
        }
        else if (StringUtils.isBlank(this.getDate())) {
            return null;
        }
        return this.getDate() + " " + this.getTime();
    }

    public String getSQLDateTimeString() {
        Instant activityInstant = this.getInstant();
        DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.of("America/New_York"));
        String activityDateTime = DATE_TIME_FORMATTER.format(activityInstant);
        return activityDateTime;
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
        else if (StringUtils.isNotBlank(this.getDateTime())){
            dateTime = this.getDateTime();
            eventTime = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.of("America/New_York")).parse(dateTime, Instant::from);
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

    public List<UPSActivity> processActivities(List<UPSActivityDto> upsActivityDtos) {
        List<UPSActivity> upsActivities = new ArrayList<>();
        upsActivityDtos.forEach(upsActivityDto -> {
            UPSLocation upsLocation = convertJsonToUpsLocation(upsActivityDto.getUpsLocation());
            UPSStatus upsStatus = new UPSStatus(upsActivityDto.getUpsStatusType(), upsActivityDto.getUpsStatusDescription(),
                    upsActivityDto.getUpsStatusCode());
            String date = upsActivityDto.getUpsActivityDateTime().toLocalDate().toString();
            String time = upsActivityDto.getUpsActivityDateTime().toLocalTime().toString();
            String dateTime = upsActivityDto.getUpsActivityDateTime().toString();
            UPSActivity upsActivity = new UPSActivity(upsLocation, upsStatus, date, time, String.valueOf(upsActivityDto.getUpsActvityId()),
                    String.valueOf(upsActivityDto.getUpsPackageId()), dateTime);
            upsActivities.add(upsActivity);
        });
        return upsActivities;
    }

    UPSLocation convertJsonToUpsLocation(String upsLocation) {
        if (StringUtils.isBlank(upsLocation)) return new UPSLocation();
        Gson gson = new Gson();
        Map<String, Map<String, String>> locationMap = gson.fromJson(upsLocation, Map.class);
        String upsAddressJson = gson.toJson(locationMap.get(ADDRESS));
        UPSAddress upsAddress = gson.fromJson(upsAddressJson, UPSAddress.class);
        return new UPSLocation(upsAddress);
    }

}
