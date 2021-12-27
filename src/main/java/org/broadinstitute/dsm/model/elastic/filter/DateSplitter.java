package org.broadinstitute.dsm.model.elastic.filter;

import org.broadinstitute.dsm.model.Filter;

public class DateSplitter extends EqualsSplitter {

    // DATE(FROM_UNIXTIME(k.scan_date/1000))  = DATE(FROM_UNIXTIME(1640563200))
    @Override
    public String[] split() {
        String[] equalSeparated = super.split();
        String leftSide = equalSeparated[0].trim();
        String rightSide = equalSeparated[1].trim();
        String fieldWithAlias = leftSide.split("\\" + Filter.OPEN_PARENTHESIS)[2].split("/")[0];
        String value = rightSide.split("\\" + Filter.OPEN_PARENTHESIS)[2].split("\\" + Filter.CLOSE_PARENTHESIS)[0];
        return new String[] {fieldWithAlias, value};
    }

}
