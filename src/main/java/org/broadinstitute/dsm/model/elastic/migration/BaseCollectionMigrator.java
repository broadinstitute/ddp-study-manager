package org.broadinstitute.dsm.model.elastic.migration;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.dsm.db.structure.ColumnName;
import org.broadinstitute.dsm.db.structure.TableName;
import org.broadinstitute.dsm.model.NameValue;
import org.broadinstitute.dsm.model.elastic.Util;
import org.broadinstitute.dsm.model.elastic.export.ElasticMappingExportAdapter;
import org.broadinstitute.dsm.model.elastic.export.RequestPayload;
import org.broadinstitute.dsm.model.elastic.export.generate.BaseGenerator;
import org.broadinstitute.dsm.model.elastic.export.generate.CollectionMappingGenerator;
import org.broadinstitute.dsm.model.elastic.export.generate.GeneratorPayload;
import org.broadinstitute.dsm.model.elastic.export.generate.MappingGenerator;
import org.broadinstitute.dsm.model.elastic.export.parse.TypeParser;
import org.broadinstitute.dsm.statics.ESObjectConstants;

public abstract class BaseCollectionMigrator extends BaseMigrator {

    private final MappingGenerator collectionMappingGenerator = new CollectionMappingGenerator();
    protected List<Map<String, Object>> transformedList;
    protected Set<String> primaryKeys;
    private Map<String, Object> endResult = new HashMap<>();

    public BaseCollectionMigrator(String index, String realm, String object) {
        super(index, realm, object);
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
        collectionMappingGenerator.setParser(new TypeParser());
        for (Object o: objects) {
            Class<?> clazz = o.getClass();
            Field[] declaredFields = clazz.getDeclaredFields();
            TableName table = clazz.getAnnotation(TableName.class);
            for (Field field: declaredFields) {
                ColumnName columnName = field.getAnnotation(ColumnName.class);
                if (columnName == null) continue;
                field.setAccessible(true);
                String fieldName = field.getName();
                String fieldNameWithAlias = table.alias() + "." + fieldName;
                try {
                    Object fieldValue = field.get(o);
                    if (fieldValue == null) continue;
                    NameValue nameValue = new NameValue(fieldNameWithAlias, fieldValue);
                    GeneratorPayload generatorPayload = new GeneratorPayload(nameValue);
                    collectionMappingGenerator.setPayload(generatorPayload);
                    endResult = collectionMappingGenerator.merge(endResult, (Map<String, Object>) collectionMappingGenerator.construct());
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }
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

    private void extractAndCollectPrimaryKey(Class<?> clazz) {
        TableName upperTable = clazz.getAnnotation(TableName.class);
        if (hasPrimaryKey(upperTable)) {
            this.primaryKeys.add(Util.underscoresToCamelCase(upperTable.primaryKey()));
        }
    }

    private boolean hasPrimaryKey(TableName table) {
        return table != null && StringUtils.isNotBlank(table.primaryKey());
    }

    private List<Field> getListTypeFields(Class<?> clazz) {
        return Arrays.stream(clazz.getDeclaredFields())
                .filter(this::isFieldListType)
                .collect(Collectors.toList());
    }

    public boolean isFieldListType(Field field) {
        return List.class.isAssignableFrom(field.getType());
    }

    private void setPrimaryId() {
        for(Map<String, Object> map: transformedList) {
            List<String> listValueKeys = getListValueKeys(map);
            for (String key : listValueKeys) {
                List<Map<String, Object>> listValue = (List<Map<String, Object>>) map.get(key);
                Optional<String> maybePrimary = getPrimaryKeyFromList(listValue);
                maybePrimary.ifPresent(prKey -> {
                    for (Map<String, Object> stringObjectMap : listValue) {
                        putPrimaryId(stringObjectMap, prKey);
                    }
                });
            }
            getPrimaryKey(map)
                    .ifPresent(outerKey -> putPrimaryId(map, outerKey));
        }
    }

    private List<String> getListValueKeys(Map<String, Object> map) {
        return map.entrySet().stream()
                .filter(this::isMapValueListType)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    private boolean isMapValueListType(Map.Entry<String, Object> entry) {
        return entry.getValue() instanceof List;
    }

    private Optional<String> getPrimaryKeyFromList(List<Map<String, Object>> listValue) {
        for(Map<String, Object> eachValue: listValue) {
            Optional<String> maybePrimaryKey = getPrimaryKey(eachValue);
            if (maybePrimaryKey.isPresent()) return maybePrimaryKey;
        }
        return Optional.empty();
    }

    private Optional<String> getPrimaryKey(Map<String, Object> map) {
        return map.keySet().stream()
                .filter(outerKey -> primaryKeys.contains(outerKey))
                .findFirst();
    }

    private void putPrimaryId(Map<String, Object> map, String outerKey) {
        map.put(Util.ID, map.get(outerKey));
    }

    @Override
    protected void exportMap() {
        ElasticMappingExportAdapter elasticMappingExportAdapter = new ElasticMappingExportAdapter();
        elasticMappingExportAdapter.setRequestPayload(new RequestPayload(index));
        elasticMappingExportAdapter.setSource(collectionMappingGenerator.getCompleteMap(endResult));
        elasticMappingExportAdapter.export();
    }
}
