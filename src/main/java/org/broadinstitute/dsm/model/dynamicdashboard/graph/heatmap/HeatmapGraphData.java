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

    public HeatmapGraphData() {}

    public HeatmapGraphData(List<HeatMapRow> rows, List<Map<String, Object>> columns,
                            Map<String, Object> colorRange, List<HeatMapDataSet> data,
                            DisplayType displayType, String displayText) {
        super(displayText, displayType);
        this.rows = rows;
        this.columns = columns;
        this.colorRange = colorRange;
        this.data = data;
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
