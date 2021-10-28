package org.broadinstitute.dsm.model.elastic.export;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SourceGenerator extends BaseGenerator {


    public SourceGenerator(Parser parser, GeneratorPayload generatorPayload) {
        super(parser, generatorPayload);
    }


    @Override
    public Map<String, Object> generate() {
        Map<String, Object> dataToExport = collect();
        return Map.of(DSM_OBJECT, dataToExport);
    }

    @Override
    protected Map<String, Object> parseJson() {
        Map<String, Object> dynamicFieldValues = parseJsonToMapFromValue();
        Map<String, Object> transformedMap = new HashMap<>();
        for (Map.Entry<String, Object> entry : dynamicFieldValues.entrySet()) {
            transformedMap.put(entry.getKey(), parser.parse(String.valueOf(entry.getValue())));
        }
        return Map.of(getOuterPropertyByAlias().getPropertyName(), transformedMap);
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
        return Map.of(
                getOuterPropertyByAlias().getPropertyName(), Map.of(getDBElement().getColumnName(),
                        element));
    }


}
