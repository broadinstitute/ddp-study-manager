package org.broadinstitute.dsm.model.elastic.export.process;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SingleProcessor extends BaseProcessor {


    @Override
    public Map<String, Object> process() {
        Map<String, Object> fetchedRecord = extractDataByReflection();

        return null;

    }

    @Override
    protected Map<String, Object> extractDataByReflection() {
        logger.info("Extracting data by field from fetched ES data");
        return null;
    }

    @Override
    protected Map<String, Object> getValueByField(Field field) {
        return null;
    }

    @Override
    protected Object convertObjectToCollection(Object object) {
        return null;
    }

    @Override
    protected String findPrimaryKeyOfObject(Object objectCollection) {
        return null;
    }

    @Override
    protected Optional<Map<String, Object>> collectEndResult() {
        return Optional.empty();
    }
}
