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
    protected Object construct() {
        logger.info("Constructing nested data");
        Map<Object, Object> collectionMap = new HashMap<>();
        collectionMap.put(Util.ID, generatorPayload.getRecordId());
        Map<String, Object> mapWithParsedObjects = parseJsonValuesToObject();
        collectionMap.putAll(mapWithParsedObjects);
        return List.of(collectionMap);
    }

    @Override
    protected Object getElement(Object element) {
        return List.of(Map.of(Util.underscoresToCamelCase(getDBElement().getColumnName()), element,
                Util.ID, generatorPayload.getRecordId()));
    }
}
