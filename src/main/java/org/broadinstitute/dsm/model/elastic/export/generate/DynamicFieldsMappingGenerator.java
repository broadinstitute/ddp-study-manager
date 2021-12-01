package org.broadinstitute.dsm.model.elastic.export.generate;

import org.broadinstitute.dsm.model.elastic.export.parse.Parser;

import java.util.HashMap;
import java.util.Map;

public class DynamicFieldsMappingGenerator implements Generator {


    private Parser parser;
    private GeneratorPayload generatorPayload;


    @Override
    public Map<String, Object> generate() {
        Object type = parser.parse(String.valueOf(generatorPayload.getValue()));
        Map<String, Object> fieldWithType = new HashMap<>(Map.of(generatorPayload.getFieldName(), type));
        return new HashMap<>(Map.of("dynamicFields", Map.of("properties", fieldWithType)));
    }

    @Override
    public void setParser(Parser parser) {
        this.parser = parser;
    }

    @Override
    public void setPayload(GeneratorPayload generatorPayload) {
        this.generatorPayload = generatorPayload;
    }
}
