package org.broadinstitute.dsm.model.elastic.export.process;

import org.broadinstitute.dsm.model.elastic.ESDsm;
import org.broadinstitute.dsm.model.elastic.export.generate.Collector;
import org.broadinstitute.dsm.model.elastic.export.generate.GeneratorPayload;
import org.broadinstitute.dsm.model.elastic.export.generate.BaseGenerator;
import org.broadinstitute.dsm.model.elastic.export.generate.MappingGenerator;
import org.broadinstitute.dsm.model.elastic.export.generate.SourceGenerator;
import org.broadinstitute.dsm.model.elastic.export.parse.Parser;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class CollectionProcessor implements Processor {

    private ESDsm esDsm;
    private String propertyName;
    private GeneratorPayload generatorPayload;
    private Collector collector;

    private final Predicate<Field> isFieldMatchProperty = field -> propertyName.equals(field.getName());

    public CollectionProcessor(ESDsm esDsm, String propertyName, GeneratorPayload generatorPayload, Collector collector) {
        this.esDsm = Objects.requireNonNull(esDsm);
        this.propertyName = Objects.requireNonNull(propertyName);
        this.generatorPayload = Objects.requireNonNull(generatorPayload);
        this.collector = collector;
    }

    protected CollectionProcessor() {

    }

    @Override
    public List<Map<String, Object>> process() {
        List<Map<String, Object>> fetchedRecords = extractDataByReflection();
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
        List<Map<String, Object>> existingRecords = fetchedRecords.stream()
                .filter(this::isExistingRecord)
                .collect(Collectors.toList());
        if (existingRecords.isEmpty()) {
            addNewRecordTo(fetchedRecords);
        } else {
            existingRecords.forEach(this::updateExistingRecord);
        }
        return fetchedRecords;
    }

    private void addNewRecordTo(List<Map<String, Object>> fetchedRecords) {
        Object collectedData = collector.collect();
        if (collectedData instanceof Map) {
            Map<String, Object> recordMap = (Map<String, Object>) collectedData;
            recordMap.put(MappingGenerator.ID, generatorPayload.getRecordId());
            fetchedRecords.add(recordMap);
        } else {
            List<Map<String, Object>> recordList = (List<Map<String, Object>>) collectedData;
            fetchedRecords.addAll(recordList);
        }
    }

    private boolean isExistingRecord(Map<String, Object> eachRecord) {
        return (double) eachRecord.get(BaseGenerator.ID) == (double) generatorPayload.getRecordId();
    }

    private void updateExistingRecord(Map<String, Object> eachRecord) {
        Object collectedData = collector.collect();
        if (collectedData instanceof Map) {
            Map<String, Object> recordMap = (Map<String, Object>) collectedData;
            recordMap.put(MappingGenerator.ID, generatorPayload.getRecordId());
            eachRecord.putAll(recordMap);
        } else {
            List<Map<String, Object>> records = (List<Map<String, Object>>) collectedData;
            records.forEach(eachRecord::putAll);
        }
    }
}
