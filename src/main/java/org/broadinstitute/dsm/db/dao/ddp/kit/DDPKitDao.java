package org.broadinstitute.dsm.db.dao.ddp.kit;

import static org.broadinstitute.ddp.db.TransactionWrapper.inTransaction;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

import org.broadinstitute.ddp.db.SimpleResult;
import org.broadinstitute.dsm.db.dao.Dao;
import org.broadinstitute.dsm.db.dto.ddp.kit.DDPKitDto;

public class DDPKitDao implements Dao<DDPKitDto> {

    private static final String SQL_DDP_KIT_BY_DSM_KIT_REQUEST_ID = "SELECT " +
            "dsm_kit_id, " +
            "dsm_kit_request_id, " +
            "tracking_to_id, " +
            "tracking_return_id, " +
            "ups_tracking_date, " +
            "ups_return_date, " +
            "ups_tracking_status, " +
            "ups_return_status, " +
            "kit_shipping_history " +
            "FROM ddp_kit WHERE dsm_kit_request_id = ?";

    public static final String DSM_KIT_ID = "dsm_kit_id";
    public static final String DSM_KIT_REQUEST_ID = "dsm_kit_request_id";
    public static final String TRACKING_TO_ID = "tracking_to_id";
    public static final String TRACKING_RETURN_ID = "tracking_return_id";
    public static final String UPS_TRACKING_DATE = "ups_tracking_date";
    public static final String UPS_RETURN_DATE = "ups_return_date";
    public static final String UPS_TRACKING_STATUS = "ups_tracking_status";
    public static final String UPS_RETURN_STATUS = "ups_return_status";
    public static final String KIT_SHIPPING_HISTORY = "kit_shipping_history";


    @Override
    public int create(DDPKitDto ddpKitDto) {
        return 0;
    }

    @Override
    public int delete(int id) {
        return 0;
    }

    @Override
    public Optional<DDPKitDto> get(long id) {
        return Optional.empty();
    }

    public Optional<DDPKitDto> getDDPKitByDsmKitRequestId(int dsmKitRequestId) {
        SimpleResult results = inTransaction((conn) -> {
            SimpleResult execResult = new SimpleResult();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_DDP_KIT_BY_DSM_KIT_REQUEST_ID)) {
                stmt.setInt(1, dsmKitRequestId);
                try(ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        execResult.resultValue = new DDPKitDto(
                                rs.getInt(DSM_KIT_ID),
                                rs.getInt(DSM_KIT_REQUEST_ID),
                                rs.getString(TRACKING_TO_ID),
                                rs.getString(TRACKING_RETURN_ID),
                                rs.getString(UPS_TRACKING_DATE),
                                rs.getString(UPS_RETURN_DATE),
                                rs.getString(UPS_TRACKING_STATUS),
                                rs.getString(UPS_RETURN_STATUS),
                                rs.getString(KIT_SHIPPING_HISTORY)
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
            throw new RuntimeException("Error getting kit with dsm kit request id: "
                    + dsmKitRequestId, results.resultException);
        }
        return Optional.ofNullable((DDPKitDto) results.resultValue);
    }
}
