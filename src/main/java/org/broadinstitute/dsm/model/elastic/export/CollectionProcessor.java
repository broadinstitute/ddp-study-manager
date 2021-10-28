package org.broadinstitute.dsm.model.elastic.export;

import org.broadinstitute.dsm.model.elastic.ESDsm;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Predicate;

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
        Map.of(propertyName, fetchedRecords);
        return updateIfExistsOrPut(fetchedRecords);
    }

    private List<Map<String, Object>> extractDataByReflection() {
        Field[] declaredFields = esDsm.getClass().getDeclaredFields();
        List<Map<String, Object>> fetchedRecords = Arrays.stream(declaredFields).filter(isFieldMatchProperty)
                .findFirst()
                .map(field -> {
                    field.setAccessible(true);
                    return getRecordsByField(field);
                })
                .orElse(new ArrayList<>());
        return fetchedRecords;
    }

    private List<Map<String, Object>> getRecordsByField(Field field) {
        try {
            return (List<Map<String, Object>>) field.get(esDsm);
        } catch (IllegalAccessException iae) {
            throw new RuntimeException("error occurred while attempting to get data from ESDsm", iae);
        }
    }

    private List<Map<String, Object>> updateIfExistsOrPut(List<Map<String, Object>> fetchedRecords) {
        fetchedRecords.stream()
                .filter(this::isExistingRecord)
                .findFirst()
                .ifPresentOrElse(this::updateExistingRecord, () -> addNewRecordTo(fetchedRecords));
        return fetchedRecords;
    }

    private void addNewRecordTo(List<Map<String, Object>> fetchedRecords) {
        fetchedRecords.add(Map.of(
                MappingGenerator.ID, generatorPayload.getRecordId(),
                generatorPayload.getNameValue().getName(), generatorPayload.getNameValue().getValue()
        ));
    }

    private boolean isExistingRecord(Map<String, Object> eachRecord) {
        return (double) eachRecord.get(BaseGenerator.ID) == (double) generatorPayload.getRecordId();
    }

    private void updateExistingRecord(Map<String, Object> eachRecord) {
        eachRecord.put(generatorPayload.getNameValue().getName(), generatorPayload.getNameValue().getValue());
    }
}
