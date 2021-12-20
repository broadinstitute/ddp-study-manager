package org.broadinstitute.dsm.model.elastic.filter;

import org.broadinstitute.dsm.model.elastic.export.parse.ValueParser;

import java.util.ArrayList;
import java.util.List;

public class FilterParser extends ValueParser {

    @Override
    public Object parse(String value) {
        if (isBoolean(value))
            return forBoolean(convertBoolean(value));
        else
            return convertString(value);
    }

    @Override
    public Object[] parse(String[] values) {
        List<Object> parsedValues = new ArrayList<>();
        for (String value : values) {
            if (isBoolean(value))
                parsedValues.add(forBoolean(convertBoolean(value)));
            else
                parsedValues.add(convertString(value));
        }
        return parsedValues.toArray();
    }

    @Override
    protected Object forNumeric(String value) {
        return value;
    }
}
