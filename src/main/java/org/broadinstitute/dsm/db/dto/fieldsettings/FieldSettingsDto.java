package org.broadinstitute.dsm.db.dto.fieldsettings;


import lombok.Data;

@Data
public class FieldSettingsDto {

    private int fieldSettingsId;
    private int ddpInstanceId;
    private String fieldType;
    private String columnName;
    private String columnDisplay;
    private String displayType;
    private String possibleValues;
    private String actions;
    private int orderNumber;
    private boolean deleted;
    private long lastChanged;
    private String changedBy;

    public FieldSettingsDto() {}
    public FieldSettingsDto(int fieldSettingsId, int ddpInstanceId, String fieldType, String columnName, String columnDisplay,
                            String displayType, String possibleValues, String actions, int orderNumber, boolean deleted, long lastChanged,
                            String changedBy) {
        this.fieldSettingsId = fieldSettingsId;
        this.ddpInstanceId = ddpInstanceId;
        this.fieldType = fieldType;
        this.columnName = columnName;
        this.columnDisplay = columnDisplay;
        this.displayType = displayType;
        this.possibleValues = possibleValues;
        this.actions = actions;
        this.orderNumber = orderNumber;
        this.deleted = deleted;
        this.lastChanged = lastChanged;
        this.changedBy = changedBy;
    }
}
