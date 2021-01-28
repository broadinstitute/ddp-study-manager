package org.broadinstitute.dsm.db;

import lombok.Data;
import lombok.NonNull;
import org.broadinstitute.ddp.db.SimpleResult;
import org.broadinstitute.dsm.db.structure.ColumnName;
import org.broadinstitute.dsm.db.structure.TableName;
import org.broadinstitute.dsm.statics.DBConstants;
import org.broadinstitute.dsm.util.DBUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.broadinstitute.ddp.db.TransactionWrapper.inTransaction;

@Data
@TableName(
        name = DBConstants.DDP_PARTICIPANT_DATA,
        alias = DBConstants.DDP_PARTICIPANT_DATA_ALIAS,
        primaryKey = DBConstants.DDP_PARTICIPANT_ID,
        columnPrefix = "")
public class ParticipantData {

    private static final Logger logger = LoggerFactory.getLogger(ParticipantData.class);

    public static final String SQL_SELECT_PARTICIPANT = "SELECT d.participant_data_id, d.ddp_participant_id, d.field_type_id, d.data " +
            "FROM ddp_participant_data d " +
            "LEFT JOIN ddp_instance realm on (d.ddp_instance_id = realm.ddp_instance_id) " +
            "WHERE realm.instance_name = ? ";

    @ColumnName(DBConstants.FIELD_TYPE_ID)
    private final String fieldTypeId;

    @ColumnName (DBConstants.DATA)
    private final String data;

    public ParticipantData(String fieldTypeId, String data) {
        this.fieldTypeId = fieldTypeId;
        this.data = data;
    }

    public static ParticipantData getParticipantDataObject(@NonNull ResultSet rs) throws SQLException {
        ParticipantData participantData = new ParticipantData(
                rs.getString(DBConstants.FIELD_TYPE_ID),
                rs.getString(DBConstants.DATA)
        );
        return participantData;
    }

    public static Map<String, List<ParticipantData>> getParticipantData(@NonNull String realm) {
        return getParticipantData(realm, null);
    }

    public static Map<String, List<ParticipantData>> getParticipantData(@NonNull String realm, String queryAddition) {
        logger.info("Collection participant data information");
        Map<String, List<ParticipantData>> participantData = new HashMap<>();
        SimpleResult results = inTransaction((conn) -> {
            SimpleResult dbVals = new SimpleResult();
            try (PreparedStatement stmt = conn.prepareStatement(DBUtil.getFinalQuery(SQL_SELECT_PARTICIPANT, queryAddition))) {
                stmt.setString(1, realm);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        String ddpParticipantId = rs.getString(DBConstants.DDP_PARTICIPANT_ID);
                        List<ParticipantData> participantDataList = new ArrayList<>();
                        if (participantData.containsKey(ddpParticipantId)) {
                            participantDataList = participantData.get(ddpParticipantId);
                        }
                        else {
                            participantData.put(ddpParticipantId, participantDataList);
                        }
                        participantDataList.add(getParticipantDataObject(rs));
                    }
                }
            }
            catch (SQLException ex) {
                dbVals.resultException = ex;
            }
            return dbVals;
        });

        if (results.resultException != null) {
            throw new RuntimeException("Couldn't get list of participant data ", results.resultException);
        }
        logger.info("Got " + participantData.size() + " participants data in DSM DB for " + realm);
        return participantData;
    }
}
