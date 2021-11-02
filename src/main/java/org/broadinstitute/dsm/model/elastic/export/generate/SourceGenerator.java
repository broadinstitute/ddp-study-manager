package org.broadinstitute.dsm.model.elastic.export.generate;

import org.broadinstitute.dsm.model.elastic.export.parse.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SourceGenerator extends BaseGenerator {

    private static final Logger logger = LoggerFactory.getLogger(SourceGenerator.class);

    public SourceGenerator(Parser parser, GeneratorPayload generatorPayload) {
        super(parser, generatorPayload);
    }


    @Override
    public Map<String, Object> generate() {
        logger.info("");
        Object dataToExport = collect();
        Map<String, Object> objectLevel = Map.of(getOuterPropertyByAlias().getPropertyName(), dataToExport);
        return Map.of(DSM_OBJECT, objectLevel);
    }

    @Override
    protected Object parseJson() {
        return constructByPropertyType();
    }

    private Map<String, Object> parseJsonValuesToObject() {
        Map<String, Object> dynamicFieldValues = parseJsonToMapFromValue();
        Map<String, Object> transformedMap = new HashMap<>();
        for (Map.Entry<String, Object> entry : dynamicFieldValues.entrySet()) {
            transformedMap.put(entry.getKey(), parser.parse(String.valueOf(entry.getValue())));
        }
        return transformedMap;
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

    @Override
    protected Object constructSingleElement() {
        return parseJsonValuesToObject();
    }

    @Override
    protected Object constructCollection() {
        Map<Object, Object> collectionMap = new HashMap<>();
        collectionMap.put(ID, generatorPayload.getRecordId());
        Map<String, Object> mapWithParsedObjects = parseJsonValuesToObject();
        collectionMap.putAll(mapWithParsedObjects);
        return List.of(collectionMap);
    }


}
