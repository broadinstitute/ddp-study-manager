package org.broadinstitute.dsm.model.elastic.export;


public interface Parser {
    Object parse(String value);
}
