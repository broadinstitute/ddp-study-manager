package org.broadinstitute.dsm.model.elastic.export.generate;

import org.broadinstitute.dsm.model.elastic.Util;
import org.broadinstitute.dsm.model.elastic.export.parse.Parser;

import java.util.HashMap;
import java.util.Map;

public class CollectionMappingGenerator extends MappingGenerator {

    public CollectionMappingGenerator(Parser parser, GeneratorPayload generatorPayload) {
        super(parser, generatorPayload);
    }

    public CollectionMappingGenerator() {}

    @Override
    protected Map<String, Object> getElement(Object type) {
        return new HashMap<>(Map.of(
                Util.ID, new HashMap<>(Map.of(TYPE, TYPE_KEYWORD)),
                Util.underscoresToCamelCase(getDBElement().getColumnName()), type
        ));
    }

    @Override
    public Map<String, Object> construct() {
        return new HashMap<>(Map.of(TYPE, NESTED, PROPERTIES, collect()));
    }

    @Override
    public Map<String, Object> merge(Map<String, Object> base, Map<String, Object> toMerge) {
        return super.merge(base, toMerge);
    }
}
