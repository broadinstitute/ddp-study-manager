package org.broadinstitute.dsm.model.elastic.export.generate;

import java.util.List;
import java.util.Map;

import org.broadinstitute.dsm.model.elastic.Util;
import org.broadinstitute.dsm.model.elastic.export.parse.Parser;

public class CollectionSourceGenerator extends SourceGenerator{
    public CollectionSourceGenerator(Parser parser,
                                     GeneratorPayload generatorPayload) {
        super(parser, generatorPayload);
    }

    @Override
    protected Object getElement(Object element) {
        return List.of(Map.of(Util.underscoresToCamelCase(getDBElement().getColumnName()), element,
                Util.ID, generatorPayload.getRecordId()));;
    }

    @Override
    protected Map<String, Object> construct() {
        return null;
    }
}
