package org.broadinstitute.dsm.db.dao.participant.data;

import static org.broadinstitute.ddp.db.TransactionWrapper.inTransaction;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.broadinstitute.ddp.db.SimpleResult;
import org.broadinstitute.dsm.db.ParticipantData;
import org.broadinstitute.dsm.db.dao.Dao;

public class ParticipantDataDao implements Dao<ParticipantData> {

    private static final String SQL_DELETE_DDP_PARTICIPANT_DATA = "DELETE FROM ddp_participant_data WHERE participant_data_id = ?";

    @Override
    public int create(ParticipantData participantData) {
        return 0;
    }

    @Override
    public int delete(int id) {
        SimpleResult results = inTransaction((conn) -> {
            SimpleResult execResult = new SimpleResult();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_DELETE_DDP_PARTICIPANT_DATA)) {
                stmt.setInt(1, id);
                execResult.resultValue = stmt.executeUpdate();
            }
            catch (SQLException ex) {
                execResult.resultException = ex;
            }
            return execResult;
        });
        if (results.resultException != null) {
            throw new RuntimeException("Error deleting participant data with "
                    + id, results.resultException);
        }
        return (int) results.resultValue;
    }
}
