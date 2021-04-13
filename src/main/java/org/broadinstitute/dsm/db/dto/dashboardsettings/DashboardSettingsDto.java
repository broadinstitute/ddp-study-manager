package org.broadinstitute.dsm.db.dto.dashboardsettings;


import lombok.Data;

@Data
public class DashboardSettingsDto {

    private int dashboardSettingsId;
    private int ddpInstanceId;
    private String displayText;
    private String displayType;
    private String possibleValues;
    private String filterType;
    private String statisticFor;
    private int orderId;

    public DashboardSettingsDto(int dashboardSettingsId, int ddpInstanceId, String displayText, String displayType,
                                String possibleValues, String filterType, String statisticFor, int orderId) {
        this.dashboardSettingsId = dashboardSettingsId;
        this.ddpInstanceId = ddpInstanceId;
        this.displayText = displayText;
        this.displayType = displayType;
        this.possibleValues = possibleValues;
        this.filterType = filterType;
        this.statisticFor = statisticFor;
        this.orderId = orderId;
    }
}
