package org.broadinstitute.dsm.model.elastic.export.process;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

import org.broadinstitute.dsm.db.structure.TableName;
import org.broadinstitute.dsm.model.elastic.ESDsm;
import org.broadinstitute.dsm.model.elastic.Util;
import org.broadinstitute.dsm.model.elastic.export.generate.Collector;
import org.broadinstitute.dsm.model.elastic.export.generate.GeneratorPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CollectionProcessor extends BaseProcessor {

    private static final Logger logger = LoggerFactory.getLogger(CollectionProcessor.class);

    private String primaryKey;

    private final Predicate<Field> isFieldMatchProperty = field -> propertyName.equals(field.getName());

    public CollectionProcessor(ESDsm esDsm, String propertyName, int recordId, Collector collector) {
        super(esDsm, propertyName, recordId, collector);
    }

    public CollectionProcessor() {

    }

    @Override
    public List<Map<String, Object>> process() {
        List<Map<String, Object>> fetchedRecords = extractDataByReflection();
        return updateIfExistsOrPut(fetchedRecords);
    }

    private List<Map<String, Object>> extractDataByReflection() {
        logger.info("Extracting data by field from fetched ES data");
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
            List<Object> objectCollection = (List<Object>) field.get(esDsm);
            primaryKey = findPrimaryKeyOfObject(objectCollection);
            return Util.convertObjectListToMapList(objectCollection);
        } catch (IllegalAccessException iae) {
            throw new RuntimeException("error occurred while attempting to get data from ESDsm", iae);
        }
    }

    private String findPrimaryKeyOfObject(List<Object> objectCollection) {
        if (Objects.isNull(objectCollection)) return "";
        Optional<Object> maybeObject = objectCollection.stream().findFirst();
        return maybeObject
                .map(o -> {
                    TableName tableName = o.getClass().getAnnotation(TableName.class);
                    return tableName != null ? tableName.primaryKey() : "";
                })
                .orElse("");
    }

    protected List<Map<String, Object>> updateIfExistsOrPut(List<Map<String, Object>> fetchedRecords) {
        fetchedRecords.stream()
                .filter(this::isExistingRecord)
                .findFirst()
                .ifPresentOrElse(this::updateExistingRecord, () -> addNewRecordTo(fetchedRecords));
        return fetchedRecords;
    }

    private boolean isExistingRecord(Map<String, Object> eachRecord) {
        if (!eachRecord.containsKey(Util.underscoresToCamelCase(primaryKey))) return false;
        double id = Double.parseDouble(String.valueOf(eachRecord.get(Util.underscoresToCamelCase(primaryKey))));
        return id == (double) recordId;
    }

    private void updateExistingRecord(Map<String, Object> eachRecord) {
        logger.info("Updating existing record");
        collectEndResult().ifPresent(eachRecord::putAll);
    }

    private void addNewRecordTo(List<Map<String, Object>> fetchedRecords) {
        logger.info("Adding new record");
        collectEndResult().ifPresent(fetchedRecords::add);
    }

    private Optional<Map<String, Object>> collectEndResult() {
        return ((List<Map<String, Object>>) collector.collect())
                .stream()
                .findFirst();
    }
}
