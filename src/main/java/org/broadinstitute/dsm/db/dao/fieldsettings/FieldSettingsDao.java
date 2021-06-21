package org.broadinstitute.dsm.db.dao.fieldsettings;

import static org.broadinstitute.ddp.db.TransactionWrapper.inTransaction;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.broadinstitute.ddp.db.SimpleResult;
import org.broadinstitute.dsm.db.dao.Dao;
import org.broadinstitute.dsm.db.dto.fieldsettings.FieldSettingsDto;

public class FieldSettingsDao implements Dao<FieldSettingsDto> {

    private static final String SQL_OPTIONS_BY_INSTANCE_ID = "SELECT " +
            "field_settings_id," +
            "ddp_instance_id," +
            "field_type," +
            "column_name," +
            "column_display," +
            "display_type," +
            "possible_values," +
            "actions," +
            "readonly," +
            "order_number," +
            "deleted," +
            "last_changed," +
            "changed_by" +
            " FROM field_settings WHERE ddp_instance_id = ? and display_type = 'OPTIONS'";

    private static final String GET_FIELD_SETTINGS = "SELECT " +
            "field_settings_id," +
            "ddp_instance_id," +
            "field_type," +
            "column_name," +
            "column_display," +
            "display_type," +
            "possible_values," +
            "actions," +
            "readonly," +
            "order_number," +
            "deleted," +
            "last_changed," +
            "changed_by" +
            " FROM field_settings";

    private static final String BY_INSTANCE_ID = " WHERE ddp_instance_id = ?";
    private static final String AND_BY_COLUMN_NAME = " AND column_name = ?";

    private static final String FIELD_SETTINGS_ID = "field_settings_id";
    private static final String DDP_INSTANCE_ID = "ddp_instance_id";
    private static final String FIELD_TYPE = "field_type";
    private static final String COLUMN_NAME = "column_name";
    private static final String COLUMN_DISPLAY = "column_display";
    private static final String DISPLAY_TYPE = "display_type";
    private static final String POSSIBLE_VALUES = "possible_values";
    private static final String ACTIONS = "actions";
    private static final String READONLY = "readonly";
    private static final String ORDER_NUMBER = "order_number";
    private static final String DELETED = "deleted";
    private static final String LAST_CHANGED = "last_changed";
    private static final String CHANGED_BY = "changed_by";

    @Override
    public int create(FieldSettingsDto fieldSettingsDto) {
        return 0;
    }

    @Override
    public int delete(int id) {
        return 0;
    }

    @Override
    public Optional<FieldSettingsDto> get(long id) {
        return Optional.empty();
    }

    public List<FieldSettingsDto> getFieldSettingsByOptionAndInstanceId(int instanceId) {
        List<FieldSettingsDto> fieldSettingsByOptions = new ArrayList<>();
        SimpleResult results = inTransaction((conn) -> {
            SimpleResult execResult = new SimpleResult();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_OPTIONS_BY_INSTANCE_ID)) {
                stmt.setInt(1, instanceId);
                try(ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        fieldSettingsByOptions.add(
                                new FieldSettingsDto(
                                        rs.getInt(FIELD_SETTINGS_ID),
                                        rs.getInt(DDP_INSTANCE_ID),
                                        rs.getString(FIELD_TYPE),
                                        rs.getString(COLUMN_NAME),
                                        rs.getString(COLUMN_DISPLAY),
                                        rs.getString(DISPLAY_TYPE),
                                        rs.getString(POSSIBLE_VALUES),
                                        rs.getString(ACTIONS),
                                        rs.getBoolean(READONLY),
                                        rs.getInt(ORDER_NUMBER),
                                        rs.getBoolean(DELETED),
                                        rs.getLong(LAST_CHANGED),
                                        rs.getString(CHANGED_BY)
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
            throw new RuntimeException("Error getting fieldSettingsByOptions for instance id: "
                    + instanceId, results.resultException);
        }
        return fieldSettingsByOptions;
    }

    public List<FieldSettingsDto>  getFieldSettingsByInstanceId(int instanceId) {
        List<FieldSettingsDto> fieldSettingsByOptions = new ArrayList<>();
        SimpleResult results = inTransaction((conn) -> {
            SimpleResult execResult = new SimpleResult();
            try (PreparedStatement stmt = conn.prepareStatement(GET_FIELD_SETTINGS + BY_INSTANCE_ID)) {
                stmt.setInt(1, instanceId);
                try(ResultSet fieldSettingsByInstanceIdRs = stmt.executeQuery()) {
                    while (fieldSettingsByInstanceIdRs.next()) {
                        fieldSettingsByOptions.add(
                                new FieldSettingsDto(
                                        fieldSettingsByInstanceIdRs.getInt(FIELD_SETTINGS_ID),
                                        fieldSettingsByInstanceIdRs.getInt(DDP_INSTANCE_ID),
                                        fieldSettingsByInstanceIdRs.getString(FIELD_TYPE),
                                        fieldSettingsByInstanceIdRs.getString(COLUMN_NAME),
                                        fieldSettingsByInstanceIdRs.getString(COLUMN_DISPLAY),
                                        fieldSettingsByInstanceIdRs.getString(DISPLAY_TYPE),
                                        fieldSettingsByInstanceIdRs.getString(POSSIBLE_VALUES),
                                        fieldSettingsByInstanceIdRs.getString(ACTIONS),
                                        fieldSettingsByInstanceIdRs.getBoolean(READONLY),
                                        fieldSettingsByInstanceIdRs.getInt(ORDER_NUMBER),
                                        fieldSettingsByInstanceIdRs.getBoolean(DELETED),
                                        fieldSettingsByInstanceIdRs.getLong(LAST_CHANGED),
                                        fieldSettingsByInstanceIdRs.getString(CHANGED_BY)
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
            throw new RuntimeException("Error getting fieldSettings ", results.resultException);
        }
        return fieldSettingsByOptions;
    }

    public Optional<FieldSettingsDto> getFieldSettingByColumnNameAndInstanceId(int instanceId, String columnName) {
        SimpleResult results = inTransaction((conn) -> {
            SimpleResult dbVals = new SimpleResult();
            try (PreparedStatement stmt = conn.prepareStatement(GET_FIELD_SETTINGS + BY_INSTANCE_ID + AND_BY_COLUMN_NAME)) {
                stmt.setInt(1, instanceId);
                stmt.setString(2, columnName);
                try(ResultSet fieldSettingsByColumnNameRs = stmt.executeQuery()) {
                    if (fieldSettingsByColumnNameRs.next()) {
                        dbVals.resultValue = new FieldSettingsDto(
                                fieldSettingsByColumnNameRs.getInt(FIELD_SETTINGS_ID),
                                fieldSettingsByColumnNameRs.getInt(DDP_INSTANCE_ID),
                                fieldSettingsByColumnNameRs.getString(FIELD_TYPE),
                                fieldSettingsByColumnNameRs.getString(COLUMN_NAME),
                                fieldSettingsByColumnNameRs.getString(COLUMN_DISPLAY),
                                fieldSettingsByColumnNameRs.getString(DISPLAY_TYPE),
                                fieldSettingsByColumnNameRs.getString(POSSIBLE_VALUES),
                                fieldSettingsByColumnNameRs.getString(ACTIONS),
                                fieldSettingsByColumnNameRs.getBoolean(READONLY),
                                fieldSettingsByColumnNameRs.getInt(ORDER_NUMBER),
                                fieldSettingsByColumnNameRs.getBoolean(DELETED),
                                fieldSettingsByColumnNameRs.getLong(LAST_CHANGED),
                                fieldSettingsByColumnNameRs.getString(CHANGED_BY)
                        );
                    }
                }
            }
            catch (SQLException ex) {
                dbVals.resultException = ex;
            }
            return dbVals;
        });
        if (results.resultException != null) {
            throw new RuntimeException("Error getting fieldSettings ", results.resultException);
        }
        return Optional.ofNullable( (FieldSettingsDto) results.resultValue);
    }
}
