package org.broadinstitute.dsm.db.dto.ups;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Date;

import lombok.Data;

@Data
public class UPSActivityDto {

    private int upsActvityId;
    private int upsPackageId;
    private String upsLocation;
    private String upsStatusType;
    private String upsStatusDescription;
    private String upsStatusCode;
    private LocalDateTime upsActivityDateTime;

    public UPSActivityDto(int upsActvityId, int upsPackageId, String upsLocation, String upsStatusType, String upsStatusDescription,
                          String upsStatusCode, Timestamp upsActivityDateTime) {
        this.upsActvityId = upsActvityId;
        this.upsPackageId = upsPackageId;
        this.upsLocation = upsLocation;
        this.upsStatusType = upsStatusType;
        this.upsStatusDescription = upsStatusDescription;
        this.upsStatusCode = upsStatusCode;
        this.upsActivityDateTime = upsActivityDateTime.toLocalDateTime();
    }

    public UPSActivityDto() {

    }
}
