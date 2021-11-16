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

        Class<?> clazz = object.getClass();
        TableName annotation = clazz.getAnnotation(TableName.class);
        if (annotation != null) {
            this.primaryKeys.add(annotation.primaryKey());
        }

        List<Field> listFields = Arrays.stream(clazz.getDeclaredFields())
                .filter(field -> List.class.isAssignableFrom(field.getType()))
                .collect(Collectors.toList());

        for (Field field : listFields) {
            try {
                List list = (List) field.get(object);
                for (Object o : list) {
                    TableName tableName = o.getClass().getAnnotation(TableName.class);
                    if (tableName != null && StringUtils.isNotBlank(tableName.primaryKey())) {
                        primaryKeys.add(tableName.primaryKey());
                        break;
                    }
                }

            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        transformedList = Util.transformObjectCollectionToCollectionMap((List) object);
        setPrimaryId();
    }

    private void setPrimaryId() {
        for(Map<String, Object> map: transformedList) {

            List<String> collect = map.entrySet().stream()
                    .filter(entry -> entry.getValue() instanceof List)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());

            for (String key : collect) {
                if (primaryKeys.contains(key)) {
                    List<Map<String, Object>> list =(List<Map<String, Object>>) map.get(key);
                    for (Map<String, Object> stringObjectMap : list) {
                        stringObjectMap.put(Util.ID, stringObjectMap.get(key));
                    }
                }
            }

            map.put(Util.ID, map.get(primaryId));
        }
    }
}
