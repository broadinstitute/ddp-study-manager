package org.broadinstitute.dsm.model.elastic.filter;


import org.broadinstitute.dsm.model.Filter;

public class LikeSplitter extends BaseSplitter {


    public LikeSplitter(String filter) {
        super(filter);
    }

    @Override
    public String[] split() {
        return filter.split(Filter.EQUALS_TRIMMED);
    }
}
