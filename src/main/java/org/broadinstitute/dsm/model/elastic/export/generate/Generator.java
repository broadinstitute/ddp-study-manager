package org.broadinstitute.dsm.model.elastic.export.generate;

import java.util.Map;

import org.broadinstitute.dsm.model.elastic.export.parse.Parser;

public interface Generator {
    Map<String, Object> generate();
    void setParser(Parser parser);
    void setPayload(GeneratorPayload generatorPayload);
}
