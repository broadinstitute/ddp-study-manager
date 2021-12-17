package org.broadinstitute.dsm.model.elastic.filter;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.dsm.model.Filter;

public class MultipleOptionsSplitter extends BaseSplitter {


    @Override
    public String[] getValue() {
        return super.getValue();
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
