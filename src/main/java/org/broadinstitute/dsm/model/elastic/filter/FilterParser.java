package org.broadinstitute.dsm.model.elastic.filter;

import org.broadinstitute.dsm.model.elastic.export.parse.ValueParser;

public class FilterParser extends ValueParser {

    @Override
    public Object parse(String value) {
        if (isBoolean(value))
            return forBoolean(convertBoolean(value));
        else
            return convertString(value);
    }

    @Override
    public Object parse(String[] values) {
        if (isBoolean(values))
            return forBoolean(convertBoolean(values));
        else
            return convertString(values);
    }

    @Override
    protected Object forNumeric(String value) {
        return value;
    }
}
