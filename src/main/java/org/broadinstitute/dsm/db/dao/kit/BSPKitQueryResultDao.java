package org.broadinstitute.dsm.db.dao.kit;

import lombok.NonNull;
import org.broadinstitute.ddp.db.SimpleResult;
import org.broadinstitute.ddp.db.TransactionWrapper;
import org.broadinstitute.dsm.db.dao.Dao;
import org.broadinstitute.dsm.db.dto.kit.BSPKitQueryResultDto;
import org.broadinstitute.dsm.model.BSPKit;
import org.broadinstitute.dsm.statics.ApplicationConfigConstants;
import org.broadinstitute.dsm.statics.DBConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Optional;

import static org.broadinstitute.ddp.db.TransactionWrapper.inTransaction;

public class BSPKitQueryResultDao implements Dao<BSPKitQueryResultDto> {


    Logger logger = LoggerFactory.getLogger(BSPKitQueryResultDao.class);

    @Override
    public int create(BSPKitQueryResultDto bspKitQueryResultDto) {
        return 0;
    }

    @Override
    public int delete(int id) {
        return 0;
    }

    @Override
    public Optional<BSPKitQueryResultDto> get(long id) {
        return Optional.empty();
    }

    public final String BASE_URL = "base_url";
    public final String BSP_SAMPLE_ID = "bsp_collaborator_sample_id";
    public final String BSP_PARTICIPANT_ID = "bsp_collaborator_participant_id";
    public final String INSTANCE_NAME = "instance_name";
    public final String BSP_COLLECTION = "bsp_collection";
    public final String BSP_ORGANISM = "bsp_organism";
    public final String DDP_PARTICIPANT_ID = "ddp_participant_id";
    public final String MATERIAL_TYPE = "bsp_material_type";
    public final String RECEPTACLE_TYPE = "bsp_receptacle_type";
    public final String PARTICIPANT_EXIT = "ddp_participant_exit_id";

    private final String BSP = "BSP";

    public final String SQL_UPDATE_KIT_RECEIVED = "UPDATE ddp_kit kit INNER JOIN( SELECT dsm_kit_request_id, MAX(dsm_kit_id) AS kit_id " +
            "FROM ddp_kit GROUP BY dsm_kit_request_id) groupedKit ON kit.dsm_kit_request_id = groupedKit.dsm_kit_request_id " +
            "AND kit.dsm_kit_id = groupedKit.kit_id SET receive_date = ?, receive_by = ? WHERE kit.receive_date IS NULL AND kit.kit_label = ?";

    public Optional<BSPKitQueryResultDto> getBSPKitQueryResult(@NonNull String kitLabel) {
        SimpleResult results = inTransaction((conn) -> {
            SimpleResult dbVals = new SimpleResult();
            try {
                try (PreparedStatement stmt = conn.prepareStatement(TransactionWrapper.getSqlFromConfig(ApplicationConfigConstants.GET_BSP_RESPONSE_INFORMATION_FOR_KIT))) {
                    stmt.setString(1, DBConstants.KIT_PARTICIPANT_NOTIFICATIONS_ACTIVATED);
                    stmt.setString(2, kitLabel);
                    try (ResultSet rs = stmt.executeQuery()) {
                        int numRows = 0;
                        while (rs.next()) {
                            numRows++;
                            dbVals.resultValue = new BSPKitQueryResultDto(
                                    rs.getString(INSTANCE_NAME),
                                    rs.getString(BASE_URL),
                                    rs.getString(BSP_SAMPLE_ID),
                                    rs.getString(BSP_PARTICIPANT_ID),
                                    rs.getString(BSP_ORGANISM),
                                    rs.getString(BSP_COLLECTION),
                                    rs.getString(DDP_PARTICIPANT_ID),
                                    rs.getString(MATERIAL_TYPE),
                                    rs.getString(RECEPTACLE_TYPE),
                                    rs.getBoolean(DBConstants.HAS_ROLE),
                                    rs.getString(PARTICIPANT_EXIT),
                                    rs.getString(DBConstants.DSM_DEACTIVATED_DATE),
                                    rs.getString(DBConstants.NOTIFICATION_RECIPIENT)
                            );
                        }
                        if (numRows > 1) {
                            throw new RuntimeException("Found " + numRows + " kits for kit label " + kitLabel);
                        }
                    }
                }
            }
            catch (Exception ex) {
                dbVals.resultException = ex;
            }
            return dbVals;
        });

        if (results.resultException != null) {
            throw new RuntimeException("Error looking up kit info for kit " + kitLabel, results.resultException);
        }
        return Optional.ofNullable((BSPKitQueryResultDto) results.resultValue);
    }

    public void setKitReceivedAndTriggerDDP(String kitLabel, boolean triggerDDP, BSPKitQueryResultDto bspKitQueryResultDto) {
        TransactionWrapper.inTransaction(conn -> {
            boolean firstTimeReceived = false;
            try (PreparedStatement stmt = conn.prepareStatement(SQL_UPDATE_KIT_RECEIVED)) {
                stmt.setLong(1, System.currentTimeMillis());
                stmt.setString(2, BSP);
                stmt.setString(3, kitLabel);
                int result = stmt.executeUpdate();
                if (result > 1) { // 1 row or 0 row updated is perfect
                    throw new RuntimeException("Error updating kit w/label " + kitLabel + " (was updating " + result + " rows)");
                }
                if (result == 1) {
                    firstTimeReceived = true;
                }
                else {
                    firstTimeReceived = false;
                }
            }
            catch (Exception e) {
                logger.error("Failed to set kit w/ label " + kitLabel + " as received ", e);
            }
            if (triggerDDP) {
                BSPKit bspKit = new BSPKit();
                bspKit.triggerDDP(conn, bspKitQueryResultDto, firstTimeReceived, kitLabel);
            }
            return firstTimeReceived;
        });
    }
}
