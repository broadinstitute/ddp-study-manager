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
import org.broadinstitute.dsm.db.dto.ups.UPSPackageDto;
import org.broadinstitute.dsm.db.dto.ups.UPShipmentDto;

public class UPSPackageDao implements Dao<UPSPackageDto> {

    private static final String SQL_UPS_PACKAGES_BY_UPS_SHIPMENT_ID = "SELECT " +
            "ups_package_id, " +
            "ups_shipment_id, " +
            "tracking_number, " +
            "delivery_date " +
            "FROM ups_package WHERE ups_shipment_id = ?";
    public static final String UPS_PACKAGE_ID = "ups_package_id";
    public static final String UPS_SHIPMENT_ID = "ups_shipment_id";
    public static final String TRACKING_NUMBER = "tracking_number";
    public static final String DELIVERY_DATE = "delivery_date";

    private static String SQL_UPS_PACKAGES_BY_UPS_SHIPMENT_IDS = "SELECT " +
            "ups_package_id, " +
            "ups_shipment_id, " +
            "tracking_number " +
            "FROM ups_package WHERE ups_shipment_id IN (?)";

    @Override
    public int create(UPSPackageDto upsPackageDto) {
        return 0;
    }

    @Override
    public int delete(int id) {
        return 0;
    }

    @Override
    public Optional<UPSPackageDto> get(long id) {
        return Optional.empty();
    }

    public List<UPSPackageDto> getUpsPackagesByUpsShipmentIds(List<Integer> upsShipmentIds) {
        List<UPSPackageDto> upsPackageDtos = new ArrayList<>();
        String sqlInClause = upsShipmentIds.stream()
                .map(i -> String.valueOf(i))
                .collect(Collectors.joining(","));
        SQL_UPS_PACKAGES_BY_UPS_SHIPMENT_IDS = SQL_UPS_PACKAGES_BY_UPS_SHIPMENT_IDS.replace("?", sqlInClause);
        SimpleResult results = inTransaction((conn) -> {
            SimpleResult execResult = new SimpleResult();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_UPS_PACKAGES_BY_UPS_SHIPMENT_IDS)) {
                try(ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        upsPackageDtos.add(
                                new UPSPackageDto(
                                        rs.getInt(UPS_PACKAGE_ID),
                                        rs.getInt(UPS_SHIPMENT_ID),
                                        rs.getString(TRACKING_NUMBER)
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
            throw new RuntimeException("Error getting ups packages", results.resultException);
        }
        return upsPackageDtos;
    }

    public List<UPSPackageDto> getUpsPackageByShipmentId(int shipmentId) {
        List<UPSPackageDto> upsPackageDtos = new ArrayList<>();
        SimpleResult results = inTransaction((conn) -> {
            SimpleResult execResult = new SimpleResult();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_UPS_PACKAGES_BY_UPS_SHIPMENT_ID)) {
                stmt.setInt(1, shipmentId);
                try(ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        upsPackageDtos.add(
                                new UPSPackageDto(
                                        rs.getInt(UPS_PACKAGE_ID),
                                        rs.getInt(UPS_SHIPMENT_ID),
                                        rs.getString(TRACKING_NUMBER),
                                        rs.getString(DELIVERY_DATE)
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
            throw new RuntimeException("Error getting ups packages by shipment id: " + shipmentId, results.resultException);
        }
        return upsPackageDtos;
    }
}
