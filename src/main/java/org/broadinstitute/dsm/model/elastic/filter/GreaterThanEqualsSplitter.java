package org.broadinstitute.dsm.model.elastic.filter;

import org.broadinstitute.dsm.model.Filter;

public class GreaterThanEqualsSplitter extends BaseSplitter {

    public GreaterThanEqualsSplitter(String filter) {
        super(filter);
    }

    @Override
    public String[] split() {
        return filter.split(Filter.LARGER_EQUALS_TRIMMED);
    }
}
