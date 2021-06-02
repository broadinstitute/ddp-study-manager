package org.broadinstitute.dsm.db.dao.ups;

import static org.broadinstitute.ddp.db.TransactionWrapper.inTransaction;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.broadinstitute.ddp.db.SimpleResult;
import org.broadinstitute.dsm.db.dao.Dao;
import org.broadinstitute.dsm.db.dto.ups.UPSActivityDto;
import org.broadinstitute.dsm.db.dto.ups.UPSPackageDto;

public class UPSActivityDao implements Dao<UPSActivityDto> {

    public static final String UPS_ACTIVITY_ID = "ups_activity_id";
    public static final String UPS_PACKAGE_ID = "ups_package_id";
    public static final String UPS_LOCATION = "ups_location";
    public static final String UPS_STATUS_TYPE = "ups_status_type";
    public static final String UPS_STATUS_DESCRIPTION = "ups_status_description";
    public static final String UPS_STATUS_CODE = "ups_status_code";
    public static final String UPS_ACTIVITY_DATE_TIME = "ups_activity_date_time";
    private static String SQL_UPS_ACTIVITIES_BY_UPS_PACKAGE_IDS = "SELECT " +
            "ups_activity_id, " +
            "ups_package_id, " +
            "ups_location, " +
            "ups_status_type, " +
            "ups_status_description, " +
            "ups_status_code, " +
            "ups_activity_date_time " +
            "FROM ups_activity WHERE ups_package_id IN (?)";

    private static String SQL_LATEST_UPS_ACTIVITY_BY_PACKAGE_ID = "SELECT " +
            "ups_activity_id, " +
            "ups_package_id, " +
            "ups_location, " +
            "ups_status_type, " +
            "ups_status_description, " +
            "ups_status_code, " +
            "ups_activity_date_time " +
            "FROM ups_activity WHERE ups_activity_id = (SELECT MAX(ups_activity_id) FROM ups_activity WHERE ups_package_id = ?)";

    @Override
    public int create(UPSActivityDto upsActivityDto) {
        return 0;
    }

    @Override
    public int delete(int id) {
        return 0;
    }

    @Override
    public Optional<UPSActivityDto> get(long id) {
        return Optional.empty();
    }

    public List<UPSActivityDto> getUpsActivitiesByUpsPackageIds(List<Integer> upsPackageIds) {
        List<UPSActivityDto> upsActivityDtos = new ArrayList<>();
        String sqlInClause = upsPackageIds.stream()
                .map(i -> String.valueOf(i))
                .collect(Collectors.joining(","));
        SQL_UPS_ACTIVITIES_BY_UPS_PACKAGE_IDS = SQL_UPS_ACTIVITIES_BY_UPS_PACKAGE_IDS.replace("?", sqlInClause);
        SimpleResult results = inTransaction((conn) -> {
            SimpleResult execResult = new SimpleResult();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_UPS_ACTIVITIES_BY_UPS_PACKAGE_IDS)) {
                try(ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        upsActivityDtos.add(
                                new UPSActivityDto(
                                        rs.getInt(UPS_ACTIVITY_ID),
                                        rs.getInt(UPS_PACKAGE_ID),
                                        rs.getString(UPS_LOCATION),
                                        rs.getString(UPS_STATUS_TYPE),
                                        rs.getString(UPS_STATUS_DESCRIPTION),
                                        rs.getString(UPS_STATUS_CODE),
                                        rs.getTimestamp(UPS_ACTIVITY_DATE_TIME).toLocalDateTime()
                                )
                        );
                    }
                }
            }
            catch (SQLException ex) {
                execResult.resultException = ex;
            }
            return execResult;
        });
        if (results.resultException != null) {
            throw new RuntimeException("Error getting ups activities", results.resultException);
        }
        return upsActivityDtos;
    }

    public Optional<UPSActivityDto> getLatestUpsActivityByPackageId(int packageId) {
        UPSActivityDto upsActivityDto = new UPSActivityDto();
        SimpleResult results = inTransaction((conn) -> {
            SimpleResult execResult = new SimpleResult();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_LATEST_UPS_ACTIVITY_BY_PACKAGE_ID)) {
                stmt.setInt(1, packageId);
                try(ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        upsActivityDto.setUpsActvityId(rs.getInt(UPS_ACTIVITY_ID));
                        upsActivityDto.setUpsPackageId(rs.getInt(UPS_PACKAGE_ID));
                        upsActivityDto.setUpsLocation(rs.getString(UPS_LOCATION));
                        upsActivityDto.setUpsStatusType(rs.getString(UPS_STATUS_TYPE));
                        upsActivityDto.setUpsStatusDescription(rs.getString(UPS_STATUS_DESCRIPTION));
                        upsActivityDto.setUpsStatusCode(rs.getString(UPS_STATUS_CODE));
                        upsActivityDto.setUpsActivityDateTime(rs.getTimestamp(UPS_ACTIVITY_DATE_TIME).toLocalDateTime());
                    }
                }
            }
            catch (SQLException ex) {
                execResult.resultException = ex;
            }
            return execResult;
        });
        if (results.resultException != null) {
            throw new RuntimeException("Error getting ups activity with package id: " + packageId, results.resultException);
        }
        return Optional.ofNullable(upsActivityDto);
    }
}
