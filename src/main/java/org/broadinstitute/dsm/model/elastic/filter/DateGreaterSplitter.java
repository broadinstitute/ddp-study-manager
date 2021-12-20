package org.broadinstitute.dsm.model.elastic.filter;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.dsm.model.Filter;

public class DateGreaterSplitter extends GreaterThanEqualsSplitter {

    @Override
    public String[] getValue() {
        return new String[]{splittedFilter[1].split(Filter.DATE_FORMAT)[1]
                .replace(Filter.OPEN_PARENTHESIS, StringUtils.EMPTY)
                .replace(Filter.CLOSE_PARENTHESIS, StringUtils.EMPTY)
                .split(",")[0]};

    }
}
