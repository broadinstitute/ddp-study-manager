package org.broadinstitute.dsm.model.elastic.filter;

import org.broadinstitute.dsm.model.Filter;

public class DiamondEqualsSplitter extends BaseSplitter {

    @Override
    protected String[] getFieldWithAlias() {
        String[] fieldWithAlias = super.getFieldWithAlias();
        String alias = fieldWithAlias[0].split(" ")[1];
        String innerProperty = fieldWithAlias[1];
        return new String[] {alias, innerProperty};
    }

    @Override
    public String[] split() {
        return filter.split(Filter.DIAMOND_EQUALS);
    }
}
