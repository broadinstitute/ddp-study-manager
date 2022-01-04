package org.broadinstitute.dsm.model.elastic.export.parse;


public interface Parser {
    Object parse(String value);

    default Object parse() { throw new UnsupportedOperationException(); }

    default Object[] parse(String[] values) {
        throw new UnsupportedOperationException();
    }
}
