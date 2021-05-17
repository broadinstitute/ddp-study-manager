package org.broadinstitute.dsm.model.dynamicdashboard;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import lombok.Data;

@Data
public class StatisticPayload {

    private DisplayType displayType;
    private String displayText;
    private StatisticFor statisticFor;
    private List<Map<String, Object>> possibleValues;
    private FilterType filterType;
    private String realm;
    private int from;
    private int to;

    private StatisticPayload(Builder builder) {
        this.displayType = builder.displayType;
        this.displayText = builder.displayText;
        this.statisticFor = builder.statisticFor;
        this.possibleValues = builder.possibleValues;
        this.filterType = builder.filterType;
        this.realm = builder.realm;
        this.from = builder.from;
        this.to = builder.to;
    }

    public static class Builder {
        //Required parameters
        private DisplayType displayType;
        private StatisticFor statisticFor;
        private FilterType filterType;

        //Optional parameters
        private String displayText = "";
        private List<Map<String, Object>> possibleValues = new ArrayList<>();
        private String realm = "";
        private int from;
        private int to;

        public Builder(DisplayType displayType, StatisticFor statisticFor, FilterType filterType) {
            this.displayType = displayType;
            this.statisticFor = statisticFor;
            this.filterType = filterType;
        }

        public Builder withDisplayText(String displayText) {
            this.displayText = displayText;
            return this;
        }

        public Builder withPossibleValues(List<Map<String, Object>> possibleValues) {
            this.possibleValues = possibleValues;
            return this;
        }

        public Builder withRealm(String realm) {
            this.realm = realm;
            return this;
        }

        public Builder withFrom(int from) {
            this.from = from;
            return this;
        }

        public Builder withTo(int to) {
            this.to = to;
            return this;
        }

        public StatisticPayload build() {
            return new StatisticPayload(this);
        }

    }
}
