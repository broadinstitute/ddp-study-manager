package org.broadinstitute.dsm.model.elastic.filter.splitter;

import org.broadinstitute.dsm.model.Filter;
import org.broadinstitute.dsm.model.elastic.filter.Operator;

public class GreaterThanEqualsSplitter extends BaseSplitter  {

    Operator greaterThanEquals = Operator.GREATER_THAN_EQUALS;

    @Override
    public String[] split() {
        return filter.split(Filter.LARGER_EQUALS_TRIMMED);
    }
}
