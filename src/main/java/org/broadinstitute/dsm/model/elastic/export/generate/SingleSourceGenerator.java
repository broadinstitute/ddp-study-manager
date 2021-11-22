package org.broadinstitute.dsm.model.elastic.export.generate;

import java.util.Map;

import org.broadinstitute.dsm.model.elastic.Util;
import org.broadinstitute.dsm.model.elastic.export.parse.Parser;

public class SingleSourceGenerator extends SourceGenerator {
    public SingleSourceGenerator(Parser parser,
                                 GeneratorPayload generatorPayload) {
        super(parser, generatorPayload);
    }

    @Override
    protected Map<String, Object> construct() {
        return null;
    }

    @Override
    protected Map<String, Object> getElement(Object element) {
        logger.info("Constructing single field with value");
        return Map.of(Util.underscoresToCamelCase(getDBElement().getColumnName()), element);
    }
}

