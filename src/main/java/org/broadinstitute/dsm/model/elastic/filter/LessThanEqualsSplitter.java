package org.broadinstitute.dsm.model.elastic.filter;

import org.broadinstitute.dsm.model.Filter;

public class LessThanEqualsSplitter extends BaseSplitter {

    @Override
    public String[] split() {
        return filter.split(Filter.SMALLER_EQUALS_TRIMMED);
    }
}
