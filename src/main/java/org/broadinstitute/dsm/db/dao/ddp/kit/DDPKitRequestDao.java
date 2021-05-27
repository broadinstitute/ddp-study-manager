package org.broadinstitute.dsm.db.dao.ddp.kit;

import static org.broadinstitute.ddp.db.TransactionWrapper.inTransaction;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.broadinstitute.ddp.db.SimpleResult;
import org.broadinstitute.dsm.db.dao.Dao;
import org.broadinstitute.dsm.db.dto.ddp.kit.DDPKitRequestDto;

public class DDPKitRequestDao implements Dao<DDPKitRequestDto> {

    private static final String SQL_KIT_REQUESTS_BY_INSTANCE_ID = "SELECT " +
            "dsm_kit_request_id, " +
            "ddp_instance_id, " +
            "ddp_kit_request_id, " +
            "ddp_participant_id " +
            "FROM ddp_kit_request WHERE ddp_instance_id = ?";

    private static final String SQL_DDP_KIT_REQUESTS_BY_DSM_KIT_REQUEST_ID =  "SELECT " +
            "dsm_kit_request_id, " +
            "ddp_instance_id, " +
            "ddp_kit_request_id, " +
            "ddp_participant_id " +
            "FROM ddp_kit_request WHERE dsm_kit_request_id = ?";

    private static final String SQL_DDP_KIT_REQUESTS_BY_PARTICIPANT_ID =  "SELECT " +
            "dsm_kit_request_id, " +
            "ddp_instance_id, " +
            "ddp_kit_request_id, " +
            "ddp_participant_id," +
            "created_by," +
            "created_date, " +
            "external_order_status, " +
            "external_order_date, " +
            "order_transmitted_at " +
            "FROM ddp_kit_request WHERE ddp_participant_id = ?";

    private static String SQL_DDP_KIT_REQUESTS_CREATED_BY_SYSTEM_BY_PARTICIPANT_IDS =  "SELECT " +
            "dsm_kit_request_id, " +
            "ddp_instance_id, " +
            "ddp_kit_request_id, " +
            "ddp_participant_id," +
            "created_by," +
            "created_date, " +
            "external_order_status, " +
            "external_order_date, " +
            "order_transmitted_at " +
            "FROM ddp_kit_request WHERE ddp_participant_id IN (?) AND created_by = 'SYSTEM'";


    private static final String DSM_KIT_REQUEST_ID = "dsm_kit_request_id";
    public static final String DDP_INSTANCE_ID = "ddp_instance_id";
    public static final String DDP_KIT_REQUEST_ID = "ddp_kit_request_id";
    public static final String DDP_PARTICIPANT_ID = "ddp_participant_id";
    public static final String CREATED_BY = "created_by";
    public static final String CREATED_DATE = "created_date";
    public static final String EXTERNAL_ORDER_STATUS = "external_order_status";
    public static final String ORDER_TRANSMITTED_AT = "order_transmitted_at";
    public static final String EXTERNAL_ORDER_DATE = "external_order_date";

    @Override
    public int create(DDPKitRequestDto ddpKitRequestDto) {
        return 0;
    }

    @Override
    public int delete(int id) {
        return 0;
    }

    @Override
    public Optional<DDPKitRequestDto> get(long id) {
        SimpleResult results = inTransaction((conn) -> {
            SimpleResult execResult = new SimpleResult();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_DDP_KIT_REQUESTS_BY_DSM_KIT_REQUEST_ID)) {
                stmt.setLong(1, id);
                try(ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        execResult.resultValue = new DDPKitRequestDto(
                                rs.getInt(DSM_KIT_REQUEST_ID),
                                rs.getInt(DDP_INSTANCE_ID),
                                rs.getString(DDP_KIT_REQUEST_ID),
                                rs.getString(CREATED_BY)
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
            throw new RuntimeException("Error getting ddp kit request with "
                    + id, results.resultException);
        }
        return Optional.ofNullable((DDPKitRequestDto) results.resultValue);
    }

    public List<DDPKitRequestDto> getKitRequestsByInstanceId(int instanceId) {
        List<DDPKitRequestDto> ddpKitRequestDtos = new ArrayList<>();
        SimpleResult results = inTransaction((conn) -> {
            SimpleResult execResult = new SimpleResult();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_KIT_REQUESTS_BY_INSTANCE_ID)) {
                stmt.setInt(1, instanceId);
                try(ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        ddpKitRequestDtos.add(
                                new DDPKitRequestDto(
                                        rs.getInt(DSM_KIT_REQUEST_ID),
                                        rs.getInt("ddp_instance_id"),
                                        rs.getString("ddp_kit_request_id"),
                                        rs.getString("ddp_participant_id")
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
            throw new RuntimeException("Error getting kit requests with instance id: "
                    + instanceId, results.resultException);
        }
        return ddpKitRequestDtos;
    }

    public List<DDPKitRequestDto> getKitRequestsByParticipantId(String participantId) {
        List<DDPKitRequestDto> ddpKitRequestDtos = new ArrayList<>();
        SimpleResult results = inTransaction((conn) -> {
            SimpleResult execResult = new SimpleResult();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_DDP_KIT_REQUESTS_BY_PARTICIPANT_ID)) {
                stmt.setString(1, participantId);
                try(ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        ddpKitRequestDtos.add(
                                new DDPKitRequestDto(
                                        rs.getInt(DSM_KIT_REQUEST_ID),
                                        rs.getInt(DDP_INSTANCE_ID),
                                        rs.getString(DDP_KIT_REQUEST_ID),
                                        rs.getString(DDP_PARTICIPANT_ID),
                                        rs.getString(CREATED_BY),
                                        rs.getLong(CREATED_DATE),
                                        rs.getString(EXTERNAL_ORDER_STATUS),
                                        rs.getLong(EXTERNAL_ORDER_DATE),
                                        rs.getTimestamp(ORDER_TRANSMITTED_AT) != null ? rs.getTimestamp(ORDER_TRANSMITTED_AT).toLocalDateTime() : null
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
            throw new RuntimeException("Error getting kit requests with participant id: "
                    + participantId, results.resultException);
        }
        return ddpKitRequestDtos;
    }

    public Map<String, List<DDPKitRequestDto>> getKitRequestsCreatedBySystemByParticipantIds(Set<String> participantIds) {
        Map<String, List<DDPKitRequestDto>> ddpKitRequestDtosMap = new HashMap<>();
        String inClause = participantIds.stream().collect(Collectors.joining("', '", "'", "'"));
        SQL_DDP_KIT_REQUESTS_CREATED_BY_SYSTEM_BY_PARTICIPANT_IDS = SQL_DDP_KIT_REQUESTS_CREATED_BY_SYSTEM_BY_PARTICIPANT_IDS
                .replace("?", inClause);
        SimpleResult results = inTransaction((conn) -> {
            SimpleResult execResult = new SimpleResult();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_DDP_KIT_REQUESTS_CREATED_BY_SYSTEM_BY_PARTICIPANT_IDS)) {
                try(ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        DDPKitRequestDto ddpKitRequestDto = new DDPKitRequestDto(
                                rs.getInt(DSM_KIT_REQUEST_ID),
                                rs.getInt(DDP_INSTANCE_ID),
                                rs.getString(DDP_KIT_REQUEST_ID),
                                rs.getString(DDP_PARTICIPANT_ID),
                                rs.getString(CREATED_BY),
                                rs.getLong(CREATED_DATE),
                                rs.getString(EXTERNAL_ORDER_STATUS),
                                rs.getLong(EXTERNAL_ORDER_DATE),
                                rs.getTimestamp(ORDER_TRANSMITTED_AT) != null ? rs.getTimestamp(ORDER_TRANSMITTED_AT).toLocalDateTime() :
                                        null
                        );
                        ddpKitRequestDtosMap.computeIfAbsent(rs.getString(DDP_PARTICIPANT_ID), v -> new ArrayList<>()).add(ddpKitRequestDto);
                    }
                }
            }
            catch (SQLException ex) {
                execResult.resultException = ex;
            }
            return execResult;
        });
        if (results.resultException != null) {
            throw new RuntimeException("Error getting kit requests with for participant ids", results.resultException);
        }
        return ddpKitRequestDtosMap;
    }
}
