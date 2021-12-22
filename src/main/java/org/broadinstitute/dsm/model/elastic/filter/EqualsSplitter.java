package org.broadinstitute.dsm.model.elastic.filter;

import org.broadinstitute.dsm.model.Filter;

public class EqualsSplitter extends BaseSplitter {

    @Override
    public String[] split() {
        return filter.split(Filter.EQUALS_TRIMMED);
    }
}
