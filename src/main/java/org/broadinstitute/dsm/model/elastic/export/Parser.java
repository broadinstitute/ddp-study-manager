package org.broadinstitute.dsm.model.elastic.export;

import org.broadinstitute.dsm.model.NameValue;

public interface Parser {
    String parseType(NameValue nameValue);
}
