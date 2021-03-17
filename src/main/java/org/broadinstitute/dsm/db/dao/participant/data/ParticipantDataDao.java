package org.broadinstitute.dsm.db.dao.participant.data;

import static org.broadinstitute.ddp.db.TransactionWrapper.inTransaction;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.broadinstitute.ddp.db.SimpleResult;
import org.broadinstitute.dsm.db.ParticipantData;
import org.broadinstitute.dsm.db.dao.Dao;
import org.broadinstitute.dsm.db.dto.participant.data.ParticipantDataDto;

public class ParticipantDataDao implements Dao<ParticipantDataDto> {

    private static final String SQL_PARTICIPANT_DATA_BY_PARTICIPANT_ID = "SELECT * FROM ddp_participant_data WHERE ddp_participant_id = ?";
    private static final String SQL_DELETE_DDP_PARTICIPANT_DATA = "DELETE FROM ddp_participant_data WHERE participant_data_id = ?";
    private static final String SQL_PARTICIPANT_DATA_BY_ID = "SELECT * FROM ddp_participant_data WHERE participant_data_id = ?";


    @Override
    public int create(ParticipantDataDto participantDataDto) {
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

    @Override
    public Optional<ParticipantDataDto> get(long id) {
        SimpleResult results = inTransaction((conn) -> {
            SimpleResult execResult = new SimpleResult();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_PARTICIPANT_DATA_BY_ID)) {
                stmt.setLong(1, id);
                try(ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                         execResult.resultValue = new ParticipantDataDto(
                                        rs.getLong(1),
                                        rs.getString(2),
                                        rs.getLong(3),
                                        rs.getString(4),
                                        rs.getString(5)
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
            throw new RuntimeException("Error getting participant data with "
                    + id, results.resultException);
        }
        return Optional.ofNullable((ParticipantDataDto) results.resultValue);
    }

    public List<ParticipantDataDto> getParticipantDataByParticipantId(String participantId) {
        List<ParticipantDataDto> participantDataDtoList = new ArrayList<>();
        SimpleResult results = inTransaction((conn) -> {
            SimpleResult execResult = new SimpleResult();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_PARTICIPANT_DATA_BY_PARTICIPANT_ID)) {
                stmt.setString(1, participantId);
                try(ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        participantDataDtoList.add(
                                new ParticipantDataDto(
                                        rs.getLong(1),
                                        rs.getString(2),
                                        rs.getLong(3),
                                        rs.getString(4),
                                        rs.getString(5)
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
            throw new RuntimeException("Error getting participant data with "
                    + participantId, results.resultException);
        }
        return participantDataDtoList;
    }
}
