package org.broadinstitute.dsm.model.elastic.export;

import java.util.Map;

import org.broadinstitute.dsm.model.NameValue;

public interface Generator {
    Map<String, Object> generate(NameValue nameValue);
}
