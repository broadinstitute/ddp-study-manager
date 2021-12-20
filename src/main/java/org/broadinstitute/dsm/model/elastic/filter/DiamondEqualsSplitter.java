package org.broadinstitute.dsm.model.elastic.filter;

import org.broadinstitute.dsm.model.Filter;

public class DiamondEqualsSplitter extends BaseSplitter {

    @Override
    protected String[] getFieldWithAlias() {
        String[] fieldWithAlias = super.getFieldWithAlias();
        String alias = splitFieldWithAliasBySpace(fieldWithAlias)[1];
        String innerProperty = fieldWithAlias[1];
        return new String[] {alias, innerProperty};
    }

    private String[] splitFieldWithAliasBySpace(String[] fieldWithAlias) {
        return fieldWithAlias[0].split(" ");
    }

    @Override
    public String[] getValue() {
        String value = "'" + super.getValue()[0] + "'";
        try {
            String not = splitFieldWithAliasBySpace(super.getFieldWithAlias())[0];
            return new String[] { not + value };
        } catch (Exception e) {
            return new String[] { value };
        }
    }

    @Override
    public String[] split() {
        return filter.split(Filter.DIAMOND_EQUALS);
    }
}
