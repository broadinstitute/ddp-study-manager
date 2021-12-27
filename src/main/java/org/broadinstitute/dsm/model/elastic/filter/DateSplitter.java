package org.broadinstitute.dsm.model.elastic.filter;

public class DateSplitter extends BaseSplitter {

    // DATE(FROM_UNIXTIME(k.scan_date/1000))  = DATE(FROM_UNIXTIME(1640563200))
    @Override
    public String[] split() {

        return null;
    }

}
