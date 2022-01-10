package org.broadinstitute.dsm.model.elastic.filter.splitter;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.dsm.model.Filter;
import org.broadinstitute.dsm.model.elastic.Util;

public class MultipleOptionsSplitter extends BaseSplitter {

    @Override
    public String[] getValue() {
        String[] values = new String[splittedFilter.length];
        for (int i = 0; i < values.length; i++) {
            String value = splittedFilter[i].split(Filter.EQUALS_TRIMMED)[1].trim();
            values[i] = value;
        }
        return values;
    }

    @Override
    public String getInnerProperty() {
        String propertyWithValue = super.getInnerProperty();
        String innerProperty = propertyWithValue.split(Filter.EQUALS_TRIMMED)[0].trim();
        return Util.underscoresToCamelCase(innerProperty);
    }

    @Override
    public String[] split() {
        String[] multipleFilters = filter
                .replace(Filter.OPEN_PARENTHESIS, StringUtils.EMPTY)
                .replace(Filter.CLOSE_PARENTHESIS, StringUtils.EMPTY)
                .trim()
                .split(Filter.OR_TRIMMED);
        return multipleFilters;
    }
}
