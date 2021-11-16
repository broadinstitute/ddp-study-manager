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
        Optional maybeObject = ((List) object).stream().findFirst();
        maybeObject.ifPresent((obj) -> {
            Class<?> clazz = obj.getClass();
            TableName annotation = clazz.getAnnotation(TableName.class);
            if (annotation != null) {
                this.primaryKeys.add(Util.underscoresToCamelCase(annotation.primaryKey()));
            }

            List<Field> listFields = Arrays.stream(clazz.getDeclaredFields())
                    .filter(field -> List.class.isAssignableFrom(field.getType()))
                    .collect(Collectors.toList());

            for (Field field : listFields) {
                for (Object rame: (List) object) {
                    try {
                        field.setAccessible(true);
                        Object o = field.get(rame);
                        Optional first = ((List) o).stream().findFirst();
                        first.ifPresent(f -> {
                            TableName tableName = f.getClass().getAnnotation(TableName.class);
                            if (tableName != null && StringUtils.isNotBlank(tableName.primaryKey())) {
                                primaryKeys.add(Util.underscoresToCamelCase(tableName.primaryKey()));
                            }
                        });
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
            }
        });


        transformedList = Util.transformObjectCollectionToCollectionMap((List) object);
        setPrimaryId();
    }

    private void setPrimaryId() {
        for(Map<String, Object> map: transformedList) {

//            List<String> collect = map.entrySet().stream()
//                    .filter(entry -> entry.getValue() instanceof List)
//                    .flatMap(entry -> ((List<Map<String, Object>>) entry.getValue()).stream())
//                    .flatMap(entry -> entry.keySet().stream())
//                    .collect(Collectors.toList());

            List<String> collect = map.entrySet().stream()
                    .filter(entry -> entry.getValue() instanceof List)
                    .map(entry -> entry.getKey())
                    .collect(Collectors.toList());

            for (String key : collect) {
                List<Map<String, Object>> o = (List<Map<String, Object>>) map.get(key);

                Optional<String> maybePrimary = o.stream()
                        .flatMap(mapObj -> mapObj.keySet().stream())
                        .filter(k -> primaryKeys.contains(k))
                        .findFirst();

                maybePrimary.ifPresent(prKey -> {
                    List<Map<String, Object>> list =(List<Map<String, Object>>) map.get(key);
                    for (Map<String, Object> stringObjectMap : list) {
                        stringObjectMap.put(Util.ID, stringObjectMap.get(prKey));
                    }
                });


            }

            map.put(Util.ID, map.get(primaryId));
        }
    }
}
