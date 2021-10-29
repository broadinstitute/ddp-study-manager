package org.broadinstitute.dsm.model.elastic.export.generate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.broadinstitute.dsm.model.elastic.export.parse.Parser;

public class SourceGenerator extends BaseGenerator {


    public SourceGenerator(Parser parser, GeneratorPayload generatorPayload) {
        super(parser, generatorPayload);
    }


    @Override
    public Map<String, Object> generate() {
        Object dataToExport = collect();
        Map<String, Object> objectLevel = Map.of(getOuterPropertyByAlias().getPropertyName(), dataToExport);
        return Map.of(DSM_OBJECT, objectLevel);
    }

    @Override
    protected Object parseJson() {
        Map<String, Object> dynamicFieldValues = parseJsonToMapFromValue();
        Map<String, Object> transformedMap = new HashMap<>();
        for (Map.Entry<String, Object> entry : dynamicFieldValues.entrySet()) {
            transformedMap.put(entry.getKey(), parser.parse(String.valueOf(entry.getValue())));
        }
        if (getOuterPropertyByAlias().isCollection()) {
            Map<Object, Object> collectionMap = new HashMap<>();
            collectionMap.put(ID, generatorPayload.getRecordId());
            collectionMap.putAll(transformedMap);
            return List.of(collectionMap);
        } else {
            return transformedMap;
        }
    }

    @Override
    protected Object parseSingleElement() {
        return getFieldWithElement();
    }

    @Override
    protected Object getElementWithId(Object element) {
        return List.of(Map.of(getDBElement().getColumnName(),element,
                ID, generatorPayload.getRecordId()));
    }

    @Override
    protected Map<String, Object> getElement(Object element) {
        return Map.of(getDBElement().getColumnName(), element);
    }


}
