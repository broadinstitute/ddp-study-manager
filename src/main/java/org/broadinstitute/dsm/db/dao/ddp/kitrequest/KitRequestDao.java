package org.broadinstitute.dsm.db.dao.ddp.kitrequest;

import org.broadinstitute.ddp.db.SimpleResult;
import org.broadinstitute.dsm.db.dao.Dao;
import org.broadinstitute.dsm.db.dto.ddp.kitrequest.ESSamplesDto;
import org.broadinstitute.dsm.db.dto.ddp.kitrequest.KitRequestDto;
import org.broadinstitute.dsm.db.dto.ddp.tissue.ESTissueRecordsDto;
import org.broadinstitute.dsm.statics.DBConstants;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.broadinstitute.ddp.db.TransactionWrapper.inTransaction;

public class KitRequestDao implements Dao<KitRequestDto> {
    @Override
    public int create(KitRequestDto kitRequestDto) {
        return 0;
    }

    @Override
    public int delete(int id) {
        return 0;
    }

    @Override
    public Optional<KitRequestDto> get(long id) {
        return Optional.empty();
    }

    public KitRequestDto getKitRequestByLabel(String query, String kitLabel) {
        List<KitRequestDto> kitRequestDtoList = new ArrayList<>();
        SimpleResult results = inTransaction((conn) -> {
            SimpleResult dbVals = new SimpleResult(0);
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, kitLabel);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        kitRequestDtoList.add(new KitRequestDto(
                                rs.getInt(DBConstants.DSM_KIT_REQUEST_ID),
                                rs.getInt(DBConstants.DDP_INSTANCE_ID),
                                rs.getString(DBConstants.DDP_KIT_REQUEST_ID),
                                rs.getInt(DBConstants.KIT_TYPE_ID),
                                rs.getString(DBConstants.BSP_COLLABORATOR_SAMPLE_ID),
                                rs.getString(DBConstants.BSP_COLLABORATOR_SAMPLE_ID),
                                rs.getString(DBConstants.DDP_PARTICIPANT_ID),
                                rs.getString(DBConstants.DDP_LABEL),
                                rs.getString(DBConstants.CREATED_BY),
                                rs.getLong(DBConstants.CREATED_DATE),
                                rs.getString(DBConstants.EXTERNAL_ORDER_NUMBER),
                                rs.getLong(DBConstants.EXTERNAL_ORDER_DATE),
                                rs.getString(DBConstants.EXTERNAL_ORDER_STATUS),
                                rs.getString(DBConstants.EXTERNAL_RESPONSE),
                                rs.getString(DBConstants.UPLOAD_REASON),
                                rs.getTimestamp(DBConstants.ORDER_TRANSMITTED_AT)
                        ));
                    }
                }
            }
            catch (SQLException ex) {
                dbVals.resultException = ex;
            }
            if (dbVals.resultException != null) {
                throw new RuntimeException("Error getting kit request with label " + kitLabel, dbVals.resultException);
            }
            return dbVals;
        });
        return kitRequestDtoList.get(0);
    }

    public List<ESSamplesDto> getESSamplesByInstanceId(int instanceId) {
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
