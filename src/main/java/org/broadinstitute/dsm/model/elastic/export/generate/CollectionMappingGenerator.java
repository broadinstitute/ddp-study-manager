package org.broadinstitute.dsm.model.elastic.export.generate;

import org.broadinstitute.dsm.model.elastic.Util;
import org.broadinstitute.dsm.model.elastic.export.parse.Parser;

import java.util.HashMap;
import java.util.Map;

public class CollectionMappingGenerator extends MappingGenerator {

    public CollectionMappingGenerator(Parser parser, GeneratorPayload generatorPayload) {
        super(parser, generatorPayload);
    }

    @Override
    protected Object getElement() {
        return Map.of(
                Util.ID, Map.of(TYPE, TYPE_KEYWORD),
                Util.underscoresToCamelCase(getDBElement().getColumnName()), type
        );
    }

    @Override
    protected Object construct() {
        return new HashMap<>(Map.of(TYPE, NESTED, PROPERTIES, collect()));
    }


}
