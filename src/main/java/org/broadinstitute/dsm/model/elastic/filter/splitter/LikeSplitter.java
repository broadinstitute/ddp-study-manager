package org.broadinstitute.dsm.model.elastic.filter.splitter;


import org.broadinstitute.dsm.model.Filter;

public class LikeSplitter extends BaseSplitter {

    @Override
    public String[] split() {
        return filter.split(Filter.LIKE_TRIMMED);
    }
}
