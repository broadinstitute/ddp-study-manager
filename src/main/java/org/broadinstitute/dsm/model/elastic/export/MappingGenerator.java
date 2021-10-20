package org.broadinstitute.dsm.model.elastic.export;

import java.util.Map;

public interface MappingGenerator {
    Map<String, Object> generate();
}
