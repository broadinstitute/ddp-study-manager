package org.broadinstitute.dsm.db.dao.ddp.kitrequest;

import org.broadinstitute.ddp.db.SimpleResult;
import org.broadinstitute.dsm.db.dao.Dao;
import org.broadinstitute.dsm.db.dto.ddp.kitrequest.ESSamplesDto;
import org.broadinstitute.dsm.db.dto.ddp.kitrequest.KitRequestDto;
import org.broadinstitute.dsm.db.dto.ddp.tissue.ESTissueRecordsDto;
import org.broadinstitute.dsm.statics.DBConstants;
import org.broadinstitute.dsm.util.SystemUtil;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.broadinstitute.ddp.db.TransactionWrapper.inTransaction;

public class KitRequestDao implements Dao<KitRequestDto> {

    public static final String SQL_SELECT_ES_SAMPLE =
            "SELECT "+
            "dp.ddp_participant_id, "+
            "kr.ddp_kit_request_id, "+
            "kt.kit_type_name, "+
            "dk.tracking_to_id, "+
            "dk.tracking_return_id, "+
            "cs.carrier, "+
            "dk.scan_date, "+
            "dk.easypost_shipment_date, "+
            "dk.receive_date "+
                    "FROM "+
            "ddp_participant dp "+
            "LEFT JOIN "+
            "ddp_kit_request kr ON dp.ddp_participant_id = kr.ddp_participant_id "+
            "LEFT JOIN "+
            "ddp_kit dk ON dk.dsm_kit_request_id = kr.dsm_kit_request_id "+
            "LEFT JOIN "+
            "kit_type kt ON kr.kit_type_id = kt.kit_type_id "+
            "LEFT JOIN "+
            "ddp_kit_request_settings krs ON (kr.ddp_instance_id = krs.ddp_instance_id "+
                    "AND kr.kit_type_id = krs.kit_type_id) "+
            "LEFT JOIN "+
            "carrier_service cs ON (krs.carrier_service_to_id = cs.carrier_service_id)";

    public static final String BY_INSTANCE_ID = " WHERE dp.ddp_instance_id = ?";

    public static final String SQL_GET_KIT_REQUEST_ID =
            "SELECT " +
            "ddp_kit_request_id " +
                    "FROM " +
            "ddp_kit_request";

    public static final String BY_BSP_COLLABORATOR_PARTICIPANT_ID = " WHERE bsp_collaborator_participant_id = ?";

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
                                rs.getString(DBConstants.COLLABORATOR_PARTICIPANT_ID),
                                rs.getString(DBConstants.BSP_COLLABORATOR_PARTICIPANT_ID),
                                rs.getString(DBConstants.DDP_PARTICIPANT_ID),
                                rs.getString(DBConstants.DSM_LABEL),
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
        List<ESSamplesDto> samplesDtosListES = new ArrayList<>();
        SimpleResult results = inTransaction((conn) -> {
            SimpleResult execResult = new SimpleResult();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_SELECT_ES_SAMPLE + BY_INSTANCE_ID)) {
                stmt.setInt(1, instanceId);
                try(ResultSet ESSampleRs = stmt.executeQuery()) {
                    while (ESSampleRs.next()) {
                        samplesDtosListES.add(
                                new ESSamplesDto(
                                        ESSampleRs.getString(DBConstants.DDP_PARTICIPANT_ID),
                                        ESSampleRs.getString(DBConstants.DDP_KIT_REQUEST_ID),
                                        ESSampleRs.getString(DBConstants.KIT_TYPE_NAME),
                                        ESSampleRs.getString(DBConstants.DSM_TRACKING_TO),
                                        ESSampleRs.getString(DBConstants.DSM_TRACKING_RETURN),
                                        ESSampleRs.getString(DBConstants.CARRIER),
                                        SystemUtil.getDateFormatted(ESSampleRs.getLong(DBConstants.DSM_SCAN_DATE)),
                                        SystemUtil.getDateFormatted(ESSampleRs.getLong(DBConstants.EASYPOST_SHIPMENT_DATE)),
                                        SystemUtil.getDateFormatted(ESSampleRs.getLong(DBConstants.DSM_RECEIVE_DATE))
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
            throw new RuntimeException("Error getting samples by instanceId " + instanceId, results.resultException);
        }
        return samplesDtosListES;
    }

    public String getKitRequestIdByBSPParticipantId(String bspParticipantId) {
        SimpleResult results = inTransaction((conn) -> {
            SimpleResult dbVals = new SimpleResult();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_GET_KIT_REQUEST_ID + BY_BSP_COLLABORATOR_PARTICIPANT_ID)) {
                stmt.setString(1, bspParticipantId);
                try (ResultSet idByBSPrs = stmt.executeQuery()) {
                    if (idByBSPrs.next()) {
                        dbVals.resultValue = idByBSPrs.getInt(DBConstants.DDP_INSTANCE_ID);
                    }
                }

                catch (SQLException e) {
                    throw new RuntimeException("Error getting information for " + bspParticipantId, e);
                }
            }
            catch (SQLException ex) {
                dbVals.resultException = ex;
            }
            return dbVals;
        });

        if (results.resultException != null) {
            throw new RuntimeException("Couldn't get kit request id for " + bspParticipantId, results.resultException);
        }
        return (String) results.resultValue;
    }
}
