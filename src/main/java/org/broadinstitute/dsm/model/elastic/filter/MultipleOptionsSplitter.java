package org.broadinstitute.dsm.model.elastic.filter;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.dsm.model.Filter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MultipleOptionsSplitter extends BaseSplitter {

    @Override
    public String[] getValue() {
        List<String> values = new ArrayList<>();
        for (String fieldValuePair : splittedFilter) {
            String value = fieldValuePair.split(Filter.EQUALS_TRIMMED)[1];
            values.add(value);
        }
        return (String[]) values.toArray();

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
