package org.broadinstitute.dsm.model.elastic.export.generate;

import org.broadinstitute.dsm.model.elastic.Util;
import org.broadinstitute.dsm.model.elastic.export.parse.Parser;

import java.util.HashMap;
import java.util.Map;

public class SingleMappingGenerator extends MappingGenerator {

    public SingleMappingGenerator(Parser parser, GeneratorPayload generatorPayload) {
        super(parser, generatorPayload);
    }

    public SingleMappingGenerator() {}

    @Override
    protected Map<String, Object> getElement(Object type) {
        return new HashMap<>(Map.of(Util.underscoresToCamelCase(getDBElement().getColumnName()), type));
    }

    @Override
    public Map<String, Object> construct() {
        return new HashMap<>(Map.of(PROPERTIES, collect()));
    }
}
