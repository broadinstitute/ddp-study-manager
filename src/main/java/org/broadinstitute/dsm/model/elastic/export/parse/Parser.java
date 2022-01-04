package org.broadinstitute.dsm.model.elastic.export.parse;


public interface Parser {
    Object parse(String value);

    Object parse();

    default Object[] parse(String[] values) {
        throw new UnsupportedOperationException();
    }
}
