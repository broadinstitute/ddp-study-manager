package org.broadinstitute.dsm.model.elastic.export.generate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.broadinstitute.dsm.model.elastic.Util;
import org.broadinstitute.dsm.model.elastic.export.parse.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CollectionSourceGenerator extends SourceGenerator {

    private static final Logger logger = LoggerFactory.getLogger(CollectionSourceGenerator.class);

    public CollectionSourceGenerator(Parser parser,
                                     GeneratorPayload generatorPayload) {
        super(parser, generatorPayload);
    }

    public CollectionSourceGenerator() {

    }

    @Override
    public Object construct() {
        logger.info("Constructing nested data");
        Map<String, Object> mapWithParsedObjects = new HashMap<>(Map.of(
                getPrimaryKey(), generatorPayload.getRecordId(),
                "dynamicFields", parseJsonValuesToObject())
        );
        return List.of(mapWithParsedObjects);
    }

    @Override
    protected Object getElement(Object element) {
        return List.of(new HashMap<>(Map.of(
                getPrimaryKey(), generatorPayload.getRecordId(),
                getFieldName(), element)
        ));
    }
}
