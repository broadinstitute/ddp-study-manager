package org.broadinstitute.dsm.db.dao.kit;

import lombok.NonNull;
import org.broadinstitute.ddp.db.SimpleResult;
import org.broadinstitute.ddp.db.TransactionWrapper;
import org.broadinstitute.dsm.db.dao.Dao;
import org.broadinstitute.dsm.db.dto.kit.BSPKitQueryResultDto;
import org.broadinstitute.dsm.statics.ApplicationConfigConstants;
import org.broadinstitute.dsm.statics.DBConstants;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Optional;

import static org.broadinstitute.ddp.db.TransactionWrapper.inTransaction;

public class BSPKitQueryResultDao implements Dao<BSPKitQueryResultDto> {


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

    public static final String BASE_URL = "base_url";
    public static final String BSP_SAMPLE_ID = "bsp_collaborator_sample_id";
    public static final String BSP_PARTICIPANT_ID = "bsp_collaborator_participant_id";
    public static final String INSTANCE_NAME = "instance_name";
    public static final String BSP_COLLECTION = "bsp_collection";
    public static final String BSP_ORGANISM = "bsp_organism";
    public static final String DDP_PARTICIPANT_ID = "ddp_participant_id";
    public static final String MATERIAL_TYPE = "bsp_material_type";
    public static final String RECEPTACLE_TYPE = "bsp_receptacle_type";
    public static final String PARTICIPANT_EXIT = "ddp_participant_exit_id";

    public static Optional<BSPKitQueryResultDto> getBSPKitQueryResult (@NonNull String kitLabel) {
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
        return Optional.of((BSPKitQueryResultDto) results.resultValue);
    }
}
