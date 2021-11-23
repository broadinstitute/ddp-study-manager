package org.broadinstitute.dsm.model.elastic.export.generate;

import java.util.Map;

import org.broadinstitute.dsm.model.elastic.export.parse.Parser;

public class MedicalRecordMappingGenerator extends CollectionMappingGenerator{
    public MedicalRecordMappingGenerator(Parser parser,
                                         GeneratorPayload generatorPayload) {
        super(parser, generatorPayload);
    }

    @Override
    public Object collect() {
        Map<String,Object> collect = (Map<String, Object>) super.collect();
        if ("followUps".equals(generatorPayload.getName())) {
            collect.remove("followUps");
        }
        return collect;
    }
}
