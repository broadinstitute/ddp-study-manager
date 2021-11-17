package org.broadinstitute.dsm.model.elastic.migration;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

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
        maybeObject.ifPresent(this::collectPrimaryKeys);
        transformedList = Util.transformObjectCollectionToCollectionMap((List) object);
        setPrimaryId();
    }

    private void collectPrimaryKeys(Object obj) {
        Class<?> clazz = obj.getClass();
        extractAndCollectPrimaryKey(clazz);
        List<Field> listFields = getListTypeFields(clazz);
        for (Field field : listFields) {
            try {
                Class<?> parameterizedType = Util.getParameterizedType(field.getGenericType());
                extractAndCollectPrimaryKey(parameterizedType);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private List<Field> getListTypeFields(Class<?> clazz) {
        return Arrays.stream(clazz.getDeclaredFields())
                .filter(this::isFieldListType)
                .collect(Collectors.toList());
    }

    private void extractAndCollectPrimaryKey(Class<?> clazz) {
        TableName upperTable = clazz.getAnnotation(TableName.class);
        if (hasPrimaryKey(upperTable)) {
            this.primaryKeys.add(Util.underscoresToCamelCase(upperTable.primaryKey()));
        }
    }

    private boolean hasPrimaryKey(TableName table) {
        return table != null && StringUtils.isNotBlank(table.primaryKey());
    }

    private boolean isFieldListType(Field field) {
        return List.class.isAssignableFrom(field.getType());
    }

    private void setPrimaryId() {
        for(Map<String, Object> map: transformedList) {
            List<String> listValueKeys = getListValueKeys(map);

            for (String key : listValueKeys) {
                // Tissue, Details -> 1 to many
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

    private List<String> getListValueKeys(Map<String, Object> map) {
        return map.entrySet().stream()
                .filter(this::isMapValueListType)
                .filter(entry -> primaryKeys.contains(entry.getKey()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    private boolean isMapValueListType(Map.Entry<String, Object> entry) {
        return entry.getValue() instanceof List;
    }
}
