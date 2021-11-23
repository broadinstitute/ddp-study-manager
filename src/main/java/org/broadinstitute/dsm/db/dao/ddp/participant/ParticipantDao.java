package org.broadinstitute.dsm.db.dao.ddp.participant;

import org.broadinstitute.ddp.db.SimpleResult;
import org.broadinstitute.dsm.db.dao.Dao;
import org.broadinstitute.dsm.db.dto.ddp.participant.ParticipantDto;
import org.broadinstitute.dsm.util.DBUtil;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

import static org.broadinstitute.ddp.db.TransactionWrapper.inTransaction;

public class ParticipantDao implements Dao<ParticipantDto> {

    private static final String SQL_INSERT_PARTICIPANT = "INSERT INTO ddp_participant (ddp_participant_id, last_version, last_version_date, ddp_instance_id, release_completed, " +
            "assignee_id_mr, assignee_id_tissue, last_changed, changed_by) VALUES (?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE last_changed = ?, changed_by = ?";
    private final String SQL_SELECT_PARTICIPANT = "SELECT p.ddp_participant_id " +
            "FROM ddp_participant p LEFT JOIN ddp_instance realm on (p.ddp_instance_id = realm.ddp_instance_id) " +
            "LEFT JOIN ddp_participant_exit ex on (p.ddp_participant_id = ex.ddp_participant_id AND p.ddp_instance_id = ex.ddp_instance_id) " +
            "LEFT JOIN ddp_institution inst on (p.participant_id = inst.participant_id) LEFT JOIN ddp_medical_record m on (m.institution_id = inst.institution_id AND NOT m.deleted <=> 1) " +
            "LEFT JOIN ddp_onc_history_detail oD on (m.medical_record_id = oD.medical_record_id AND NOT oD.deleted <=> 1) " +
            "LEFT JOIN ddp_tissue t on (t.onc_history_detail_id = oD.onc_history_detail_id AND NOT t.deleted <=> 1) " ;

    @Override
    public int create(ParticipantDto participantDto) {
        SimpleResult simpleResult = inTransaction(conn -> {
            SimpleResult dbVals = new SimpleResult(-1);
            try (PreparedStatement stmt = conn.prepareStatement(SQL_INSERT_PARTICIPANT, Statement.RETURN_GENERATED_KEYS )) {
                stmt.setString(1, participantDto.getDdpParticipantId().orElse(null));
                stmt.setObject(2, participantDto.getLastVersion().orElse(null));
                stmt.setString(3, participantDto.getLastVersionDate().orElse(null));
                stmt.setInt(4, participantDto.getDdpInstanceId());
                stmt.setObject(5, participantDto.getReleaseCompleted().orElse(null));
                stmt.setObject(6, participantDto.getAssigneeIdMr().orElse(null));
                stmt.setObject(7, participantDto.getAssigneeIdTissue().orElse(null));
                stmt.setObject(8, participantDto.getAssigneeIdTissue().orElse(null));
                stmt.setLong(8, participantDto.getLastChanged());
                stmt.setObject(9, participantDto.getChangedBy().orElse(null));
                stmt.setLong(10, participantDto.getLastChanged());
                stmt.setObject(11, participantDto.getChangedBy().orElse(null));
                stmt.executeUpdate();
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        dbVals.resultValue = rs.getInt(1);
                    }
                }
            } catch (SQLException sqle) {
                dbVals.resultException = sqle;
            }
            return dbVals;
        });
        if (simpleResult.resultException != null) {
            throw new RuntimeException("Error inserting participant with id: " + participantDto.getDdpParticipantId().orElse(""), simpleResult.resultException);
        }
        return (int) simpleResult.resultValue;
    }

    @Override
    public int delete(int id) {
        return 0;
    }

    @Override
    public Optional<ParticipantDto> get(long id) {
        return Optional.empty();
    }

    public String getDDPParticipantId(String condition, List<String> values){
        SimpleResult simpleResult = inTransaction(conn -> {
            SimpleResult dbVals = new SimpleResult(-1);
            try (PreparedStatement stmt = conn.prepareStatement(DBUtil.getFinalQuery(SQL_SELECT_PARTICIPANT, condition))) {
                for (int i=0; i<values.size(); i++)
                    stmt.setString(i+1, values.get(i));
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        dbVals.resultValue = rs.getInt(1);
                    }
                }
            } catch (SQLException sqle) {
                dbVals.resultException = sqle;
            }
            return dbVals;
        });
        if (simpleResult.resultException != null) {
            throw new RuntimeException("Error getting participant id: ", simpleResult.resultException);
        }
        return (String) simpleResult.resultValue;
    }
}
