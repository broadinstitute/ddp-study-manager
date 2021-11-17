package org.broadinstitute.dsm.model.elastic.migration;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.dsm.db.structure.TableName;
import org.broadinstitute.dsm.model.elastic.Util;
import org.broadinstitute.dsm.statics.ESObjectConstants;

public abstract class BaseCollectionMigrator extends BaseMigrator {

    protected String primaryId;
    protected List<Map<String, Object>> transformedList;
    protected Set<String> primaryKeys;

    public BaseCollectionMigrator(String index, String realm, String object, String primaryId) {
        super(index, realm, object);
        this.primaryId = primaryId;
        this.primaryKeys = new HashSet<>();
    }

    @Override
    public Map<String, Object> generate() {
        return Map.of(ESObjectConstants.DSM, Map.of(object, transformedList));
    }

    @Override
    protected void transformObject(Object object) {
        List<Object> objects = (List<Object>) object;
        Optional<Object> maybeObject = objects.stream().findFirst();
        maybeObject.ifPresent((obj) -> collectPrimaryKeys(objects, obj));
        transformedList = Util.transformObjectCollectionToCollectionMap((List) object);
        setPrimaryId();
    }

    private void collectPrimaryKeys(List<Object> objects, Object obj) {
        Class<?> clazz = obj.getClass();
        TableName upperTable = clazz.getAnnotation(TableName.class);
        if (hasPrimaryKey(upperTable)) {
            this.primaryKeys.add(Util.underscoresToCamelCase(upperTable.primaryKey()));
        }

        List<Field> listFields = Arrays.stream(clazz.getDeclaredFields())
                .filter(this::isListType)
                .collect(Collectors.toList());

        for (Field field : listFields) {
            // listfields = Tissue, ragaca

            field.getGenericType(); 

            for (Object eachObject: objects) {
                try {
                    field.setAccessible(true);
                    List<Object> fieldValue = (List<Object>) field.get(eachObject);
                    Optional<Object> first = fieldValue.stream().findFirst();
                    first.ifPresent(instance -> {
                        TableName innerTable = instance.getClass().getAnnotation(TableName.class);
                        if (hasPrimaryKey(innerTable)) {
                            primaryKeys.add(Util.underscoresToCamelCase(innerTable.primaryKey()));
                        }
                    });
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private boolean hasPrimaryKey(TableName table) {
        return table != null && StringUtils.isNotBlank(table.primaryKey());
    }

    private boolean isListType(Field field) {
        return List.class.isAssignableFrom(field.getType());
    }

    private void setPrimaryId() {
        for(Map<String, Object> map: transformedList) {

            List<String> listValueKeys = map.entrySet().stream()
                    .filter(entry -> entry.getValue() instanceof List)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());

            for (String key : listValueKeys) {
                List<Map<String, Object>> listValue = (List<Map<String, Object>>) map.get(key);

                Optional<String> maybePrimary = listValue.stream()
                        .flatMap(mapObj -> mapObj.keySet().stream())
                        .filter(k -> primaryKeys.contains(k))
                        .findFirst();

                maybePrimary.ifPresent(prKey -> {
                    for (Map<String, Object> stringObjectMap : listValue) {
                        stringObjectMap.put(Util.ID, stringObjectMap.get(prKey));
                    }
                });
            }

            map.put(Util.ID, map.get(primaryId));
        }
    }
}
