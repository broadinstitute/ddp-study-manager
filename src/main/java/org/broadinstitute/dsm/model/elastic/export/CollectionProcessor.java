package org.broadinstitute.dsm.model.elastic.export;

import org.broadinstitute.dsm.model.elastic.ESDsm;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class CollectionProcessor implements Processor {

    private ESDsm esDsm;
    private String propertyName;
    private GeneratorPayload generatorPayload;

    private final Predicate<Field> isFieldMatchProperty = field -> propertyName.equals(field.getName());

    public CollectionProcessor(ESDsm esDsm, String propertyName, GeneratorPayload generatorPayload) {
        this.esDsm = Objects.requireNonNull(esDsm);
        this.propertyName = Objects.requireNonNull(propertyName);
        this.generatorPayload = Objects.requireNonNull(generatorPayload);
    }

    @Override
    public List<Map<String, Object>> process() {
        List<Map<String, Object>> fetchedRecords = extractDataByReflection();
        return updateIfExists(fetchedRecords);
    }

    private List<Map<String, Object>> extractDataByReflection() {
        Field[] declaredFields = esDsm.getClass().getDeclaredFields();
        Field field = Arrays.stream(declaredFields).filter(isFieldMatchProperty)
                .findFirst()
                .orElseThrow();
        field.setAccessible(true);
        List<Map<String, Object>> fetchedRecords;
        try {
            fetchedRecords = (List<Map<String, Object>>) field.get(esDsm);
        } catch (IllegalAccessException iae) {
            throw new RuntimeException("error occurred while attempting to get data from ESDsm", iae);
        }
        if (fetchedRecords == null) {
            fetchedRecords = Collections.emptyList();
        }
        return fetchedRecords;
    }

    private List<Map<String, Object>> updateIfExists(List<Map<String, Object>> fetchedRecords) {
        return fetchedRecords.stream()
                .filter(this::isExistingRecord)
                .map(this::updateExistingRecord)
                .collect(Collectors.toList());
    }

    private boolean isExistingRecord(Map<String, Object> eachRecord) {
        return (int) eachRecord.get(BaseGenerator.ID) == generatorPayload.getRecordId();
    }

    private Map<String, Object> updateExistingRecord(Map<String, Object> eachRecord) {
        eachRecord.put(generatorPayload.getNameValue().getName(), generatorPayload.getNameValue().getValue());
        return eachRecord;
    }
}
