package org.broadinstitute.dsm.db.dao.ddp.tissue;

import org.broadinstitute.ddp.db.SimpleResult;
import org.broadinstitute.dsm.db.dao.Dao;
import org.broadinstitute.dsm.db.dto.ddp.tissue.ESTissueRecordsDto;
import org.broadinstitute.dsm.statics.DBConstants;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.broadinstitute.ddp.db.TransactionWrapper.inTransaction;

public class ESTissueRecordsDao implements Dao<ESTissueRecordsDto> {

    public static final String SQL_SELECT_ES_TISSUE_RECORD =
            "SELECT "+
            "dp.participant_id, "+
            "dt.tissue_id, "+
            "onc.type_px, "+
            "onc.location_px, "+
            "onc.date_px, "+
            "onc.histology, "+
            "onc.accession_number, "+
            "onc.fax_sent, "+
            "onc.tissue_received, "+
            "dt.sent_gp "+
                    "from "+
            "ddp_tissue dt "+
            "left join ddp_onc_history_detail onc "+
            "on "+
            "onc.onc_history_detail_id = dt.onc_history_detail_id "+
            "left join ddp_medical_record mr "+
            "on mr.medical_record_id = onc.medical_record_id "+
            "LEFT JOIN "+
            "ddp_institution di ON mr.institution_id = di.institution_id "+
            "LEFT JOIN "+
            "ddp_participant dp ON di.participant_id = dp.participant_id ";

    public static final String BY_INSTANCE_ID = " WHERE dp.ddp_instance_id = ?";

    @Override
    public int create(ESTissueRecordsDto esTissueRecordsDto) {
        return 0;
    }

    @Override
    public int delete(int id) {
        return 0;
    }

    @Override
    public Optional<ESTissueRecordsDto> get(long id) {
        return Optional.empty();
    }

    public List<ESTissueRecordsDto> getESTissueRecordsByInstanceId(int instanceId) {
        List<ESTissueRecordsDto> tissueRecordsDtoListES = new ArrayList<>();
        SimpleResult results = inTransaction((conn) -> {
            SimpleResult execResult = new SimpleResult();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_SELECT_ES_TISSUE_RECORD + BY_INSTANCE_ID)) {
                stmt.setInt(1, instanceId);
                try(ResultSet ESmrRs = stmt.executeQuery()) {
                    while (ESmrRs.next()) {
                        tissueRecordsDtoListES.add(
                                new ESTissueRecordsDto(
                                        ESmrRs.getString(DBConstants.DDP_PARTICIPANT_ID),
                                        ESmrRs.getInt(DBConstants.TISSUE_ID),
                                        ESmrRs.getString(DBConstants.TYPE_PX),
                                        ESmrRs.getString(DBConstants.LOCATION_PX),
                                        ESmrRs.getString(DBConstants.DATE_PX),
                                        ESmrRs.getString(DBConstants.HISTOLOGY),
                                        ESmrRs.getString(DBConstants.ACCESSION_NUMBER),
                                        ESmrRs.getString(DBConstants.FAX_SENT),
                                        ESmrRs.getString(DBConstants.TISSUE_RECEIVED),
                                        ESmrRs.getString(DBConstants.SENT_GP)
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
            throw new RuntimeException("Error getting medical records by instanceId " + instanceId, results.resultException);
        }
        return tissueRecordsDtoListES;
    }
}
