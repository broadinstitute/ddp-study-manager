package org.broadinstitute.dsm.model.elastic.export.generate;

import java.util.Map;

import org.broadinstitute.dsm.model.elastic.export.parse.Parser;

public class NullObjectGenerator implements Generator {
    @Override
    public Map<String, Object> generate() {
        return Map.of();
    }

    @Override
    public void setParser(Parser parser) {

    }

    @Override
    public void setPayload(GeneratorPayload generatorPayload) {

    }
}
