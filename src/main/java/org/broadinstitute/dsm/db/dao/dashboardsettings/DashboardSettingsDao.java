package org.broadinstitute.dsm.db.dao.dashboardsettings;

import static org.broadinstitute.ddp.db.TransactionWrapper.inTransaction;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.broadinstitute.ddp.db.SimpleResult;
import org.broadinstitute.dsm.db.dao.Dao;
import org.broadinstitute.dsm.db.dto.dashboardsettings.DashboardSettingsDto;
import org.broadinstitute.dsm.db.dto.participant.data.ParticipantDataDto;

public class DashboardSettingsDao implements Dao<DashboardSettingsDto> {

    private static String SQL_DASHBOARD_SETTINGS_BY_INSTANCE_ID = "SELECT " +
            "dashboard_settings_id, " +
            "ddp_instance_id, " +
            "display_text, " +
            "display_type, " +
            "possible_values, " +
            "filter_type, " +
            "statistic_for, " +
            "order_id " +
            "FROM dashboard_settings WHERE ddp_instance_id = ?";

    private static final String DASHBOARD_SETTINGS_ID = "dashboard_settings_id";
    private static final String DDP_INSTANCE_ID = "ddp_instance_id";
    private static final String DISPLAY_TEXT = "display_text";
    private static final String POSSIBLE_VALUES = "possible_values";
    private static final String FILTER_TYPE = "filter_type";
    private static final String ORDER_ID = "order_id";
    private static final String DISPLAY_TYPE = "display_type";
    private static final String STATISTIC_FOR = "statistic_for";


    @Override
    public int create(DashboardSettingsDto dashboardSettingsDto) {
        return 0;
    }

    @Override
    public int delete(int id) {
        return 0;
    }

    @Override
    public Optional<DashboardSettingsDto> get(long id) {
        return Optional.empty();
    }

    public List<DashboardSettingsDto> getDashboardSettingsByInstanceId(int instanceId) {
        List<DashboardSettingsDto> dashboardSettingsDtos = new ArrayList<>();
        SimpleResult results = inTransaction((conn) -> {
            SimpleResult execResult = new SimpleResult();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_DASHBOARD_SETTINGS_BY_INSTANCE_ID)) {
                stmt.setInt(1, instanceId);
                try(ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        dashboardSettingsDtos.add(
                                new DashboardSettingsDto(
                                        rs.getInt(DASHBOARD_SETTINGS_ID),
                                        rs.getInt(DDP_INSTANCE_ID),
                                        rs.getString(DISPLAY_TEXT),
                                        rs.getString(DISPLAY_TYPE),
                                        rs.getString(POSSIBLE_VALUES),
                                        rs.getString(FILTER_TYPE),
                                        rs.getString(STATISTIC_FOR),
                                        rs.getInt(ORDER_ID)
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
            throw new RuntimeException("Error getting dashboard data with instance id: "
                    + instanceId, results.resultException);
        }
        return dashboardSettingsDtos;
    }
}
