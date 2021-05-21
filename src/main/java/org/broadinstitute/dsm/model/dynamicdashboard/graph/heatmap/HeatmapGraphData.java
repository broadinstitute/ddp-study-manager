package org.broadinstitute.dsm.model.dynamicdashboard.graph.heatmap;

import java.util.List;
import java.util.Map;

import lombok.Data;
import org.broadinstitute.dsm.model.dynamicdashboard.DisplayType;
import org.broadinstitute.dsm.model.dynamicdashboard.graph.GraphResult;

@Data
public class HeatmapGraphData extends GraphResult {

    private List<HeatMapRow> rows;
    private List<Map<String, Object>> columns;
    private Map<String, Object> colorRange;
    private List<HeatMapDataSet> data;

    private HeatmapGraphData(Builder builder) {
        this.displayText = builder.displayText;
        this.displayType = builder.displayType;
        this.dashboardSettingId = builder.dashboardSettingId;
        this.rows = builder.rows;
        this.columns = builder.columns;
        this.colorRange = builder.colorRange;
        this.data = builder.data;
    }

    public static class Builder {

        private List<HeatMapRow> rows;
        private List<Map<String, Object>> columns;
        private Map<String, Object> colorRange;
        private List<HeatMapDataSet> data;

        //optional fields
        private String displayText;
        private DisplayType displayType;
        private int dashboardSettingId;

        public Builder(List<HeatMapRow> rows, List<Map<String, Object>> columns,
                       Map<String, Object> colorRange, List<HeatMapDataSet> data) {
            this.rows = rows;
            this.columns = columns;
            this.colorRange = colorRange;
            this.data = data;
        }

        public Builder withDisplayText(String displayText) {
            this.displayText = displayText;
            return this;
        }

        public Builder withDisplayType(DisplayType displayType) {
            this.displayType = displayType;
            return this;
        }

        public Builder withDashboardSettingId(int dashboardSettingId) {
            this.dashboardSettingId = dashboardSettingId;
            return this;
        }

        public HeatmapGraphData build() {
            return new HeatmapGraphData(this);
        }

    }

    @Data
    public static class HeatMapRow {
        private String id;
        private String label;
        private String guid;

        public HeatMapRow(String id, String label, String guid) {
            this.id = id;
            this.label = label;
            this.guid = guid;
        }
    }

    @Data
    public static class HeatMapDataSet {
        private String rowId;
        private String columnId;
        private String colorRangeLabel;

        public HeatMapDataSet(String rowId, String columnId, String colorRangeLabel) {
            this.rowId = rowId;
            this.columnId = columnId;
            this.colorRangeLabel = colorRangeLabel;
        }
    }

}
