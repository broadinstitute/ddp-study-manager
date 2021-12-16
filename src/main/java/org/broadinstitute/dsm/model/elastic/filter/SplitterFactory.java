package org.broadinstitute.dsm.model.elastic.filter;

import org.broadinstitute.dsm.model.Filter;

public class SplitterFactory {

    public static BaseSplitter createSplitter(String filter) {
        BaseSplitter splitter;
        if (filter.contains(Filter.EQUALS_TRIMMED))
            splitter = new EqualsSplitter(filter);
        else if (filter.contains(Filter.LIKE_TRIMMED))
            splitter = new LikeSplitter(filter);
        else if (filter.contains(Filter.LARGER_EQUALS_TRIMMED))
            splitter = new GreaterThanEqualsSplitter(filter);
        else if (filter.contains(Filter.SMALLER_EQUALS_TRIMMED))
            splitter = new LessThanEqualsSplitter(filter);
        else if (filter.contains(Filter.IS_NOT_NULL.trim()))
            splitter = new IsNotNullSplitter(filter);
        else
            throw new IllegalArgumentException("Unknown operator");
        return splitter;
    }
}
