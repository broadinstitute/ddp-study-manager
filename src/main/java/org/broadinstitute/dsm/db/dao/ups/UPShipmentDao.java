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
import org.broadinstitute.dsm.db.dto.ups.UPShipmentDto;

public class UPShipmentDao implements Dao<UPShipmentDto> {

    private static final String SQL_UPS_SHIPMENT_BY_DSM_KIT_REQUEST_ID = "SELECT " +
            "ups_shipment_id, " +
            "dsm_kit_request_id " +
            "FROM ups_shipment WHERE dsm_kit_request_id = ?";

    private static String SQL_UPS_SHIPMENTS_BY_DSM_KIT_REQUEST_IDS = "SELECT " +
            "ups_shipment_id, " +
            "dsm_kit_request_id " +
            "FROM ups_shipment WHERE dsm_kit_request_id IN (?)";

    private static String SQL_UPS_SHIPMENT_BY_SHIPMENT_ID = "SELECT " +
            "ups_shipment_id, " +
            "dsm_kit_request_id, " +
            "FROM ups_shipment WHERE dsm_kit_request_id = ?";

    public static final String UPS_SHIPMENT_ID = "ups_shipment_id";
    public static final String DSM_KIT_REQUEST_ID = "dsm_kit_request_id";

    @Override
    public int create(UPShipmentDto upShipmentDto) {
        return 0;
    }

    @Override
    public int delete(int id) {
        return 0;
    }

    @Override
    public Optional<UPShipmentDto> get(long id) {
        SimpleResult results = inTransaction((conn) -> {
            SimpleResult execResult = new SimpleResult();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_UPS_SHIPMENT_BY_SHIPMENT_ID)) {
                stmt.setLong(1, id);
                try(ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        execResult.resultValue = new UPShipmentDto(
                            rs.getInt(UPS_SHIPMENT_ID),
                            rs.getInt(DSM_KIT_REQUEST_ID)
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
            throw new RuntimeException("Error getting ups shipment with id: "
                    + id, results.resultException);
        }
        return Optional.ofNullable((UPShipmentDto) results.resultValue);
    }


    public List<UPShipmentDto> getUPShipmentsByDsmKitRequestIds(List<Integer> dsmKitRequestIds) {
        List<UPShipmentDto> upShipmentDtos = new ArrayList<>();
        String sqlInClause = dsmKitRequestIds.stream()
                .map(i -> String.valueOf(i))
                .collect(Collectors.joining(","));
        SQL_UPS_SHIPMENTS_BY_DSM_KIT_REQUEST_IDS = SQL_UPS_SHIPMENTS_BY_DSM_KIT_REQUEST_IDS.replace("?", sqlInClause);
        SimpleResult results = inTransaction((conn) -> {
            SimpleResult execResult = new SimpleResult();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_UPS_SHIPMENTS_BY_DSM_KIT_REQUEST_IDS)) {
                try(ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        upShipmentDtos.add(
                                new UPShipmentDto(
                                        rs.getInt(UPS_SHIPMENT_ID),
                                        rs.getInt(DSM_KIT_REQUEST_ID)
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
            throw new RuntimeException("Error getting ups shipments", results.resultException);
        }
        return upShipmentDtos;
    }

    public Optional<UPShipmentDto> getUpshipmentByDsmKitRequestId(int dsmKitRequestId) {
        UPShipmentDto upShipmentDto;
        SimpleResult results = inTransaction((conn) -> {
            SimpleResult execResult = new SimpleResult();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_UPS_SHIPMENT_BY_DSM_KIT_REQUEST_ID)) {
                stmt.setInt(1, dsmKitRequestId);
                try(ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        execResult.resultValue = new UPShipmentDto(
                                rs.getInt(UPS_SHIPMENT_ID),
                                rs.getInt(DSM_KIT_REQUEST_ID)
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
            throw new RuntimeException("Error getting ups shipment by dsm kit request id: " + dsmKitRequestId, results.resultException);
        }
        upShipmentDto = (UPShipmentDto) results.resultValue;
        return Optional.ofNullable(upShipmentDto);
    }
}
