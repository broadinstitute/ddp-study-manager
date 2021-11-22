package org.broadinstitute.dsm.model.elastic.export.generate;

import org.broadinstitute.dsm.model.elastic.export.parse.Parser;

import java.util.HashMap;
import java.util.Map;

public class SingleMappingGenerator extends MappingGenerator {

    public SingleMappingGenerator(Parser parser, GeneratorPayload generatorPayload) {
        super(parser, generatorPayload);
    }

    @Override
    protected Object getElement() {
        return null;
    }

    @Override
    protected Object construct() {
        return new HashMap<>(Map.of(PROPERTIES, collect()));
    }
}
