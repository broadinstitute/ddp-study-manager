package org.broadinstitute.dsm.model.elastic.filter;

import org.broadinstitute.dsm.model.elastic.export.parse.ValueParser;

public class FilterParser extends ValueParser {

    @Override
    protected Object forNumeric(String value) {
        return value;
    }
}
