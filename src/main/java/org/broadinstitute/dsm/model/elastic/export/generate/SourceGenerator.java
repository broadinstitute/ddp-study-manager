package org.broadinstitute.dsm.model.elastic.export.generate;

import org.broadinstitute.dsm.model.elastic.Util;
import org.broadinstitute.dsm.model.elastic.export.parse.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

abstract public class SourceGenerator extends BaseGenerator {

    private static final Logger logger = LoggerFactory.getLogger(SourceGenerator.class);

    public SourceGenerator(Parser parser, GeneratorPayload generatorPayload) {
        super(parser, generatorPayload);
    }


    @Override
    public Map<String, Object> generate() {
        Object dataToExport = collect();
        logger.info("Generating final source");
        return Map.of(DSM_OBJECT, buildPropertyLevelWithData(dataToExport));
    }

    private Map<String, Object> buildPropertyLevelWithData(Object dataToExport) {
        return Map.of(getOuterPropertyByAlias().getPropertyName(), dataToExport);
    }

    @Override
    protected Object parseJson() {
        return construct();
    }

    protected Map<String, Object> parseJsonValuesToObject() {
        logger.info("Converting JSON values to Map");
        Map<String, Object> dynamicFieldValues = parseJsonToMapFromValue();
        Map<String, Object> transformedMap = new HashMap<>();
        for (Map.Entry<String, Object> entry : dynamicFieldValues.entrySet()) {
            transformedMap.put(Util.underscoresToCamelCase(entry.getKey()), parser.parse(String.valueOf(entry.getValue())));
        }
        return transformedMap;
    }

}
