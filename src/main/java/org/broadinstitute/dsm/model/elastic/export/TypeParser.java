package org.broadinstitute.dsm.model.elastic.export;

public class TypeParser extends BaseParser {


    @Override
    protected Object forNumeric(String value) {
        return "long";
    }

    @Override
    protected Object forBoolean(String value) {
        return "boolean";
    }

    @Override
    protected Object forDate(String value) {
        return "date";
    }

    @Override
    protected Object forString(String value) {
        return "text";
    }
}