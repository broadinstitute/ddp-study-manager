package org.broadinstitute.dsm.model.elastic.export.generate;

import java.util.Map;

import org.broadinstitute.dsm.model.elastic.Util;
import org.broadinstitute.dsm.model.elastic.export.parse.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SingleSourceGenerator extends SourceGenerator {

    private static final Logger logger = LoggerFactory.getLogger(SingleSourceGenerator.class);

    public SingleSourceGenerator(Parser parser,
                                 GeneratorPayload generatorPayload) {
        super(parser, generatorPayload);
    }

    public SingleSourceGenerator() {

    }

    @Override
    protected Object construct() {
        return parseJsonValuesToObject();
    }

    @Override
    protected Map<String, Object> getElement(Object element) {
        logger.info("Constructing single field with value");
        return Map.of(Util.underscoresToCamelCase(getDBElement().getColumnName()), element);
    }
}

