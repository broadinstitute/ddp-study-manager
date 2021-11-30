package org.broadinstitute.dsm.model.elastic.migration;

import org.broadinstitute.dsm.model.elastic.Util;
import org.broadinstitute.dsm.statics.ESObjectConstants;

import java.util.*;
import java.util.stream.Collectors;

public abstract class BaseCollectionMigrator extends BaseMigrator {

    protected List<Map<String, Object>> transformedList;
    protected Set<String> primaryKeys;

    public BaseCollectionMigrator(String index, String realm, String object) {
        super(index, realm, object);
        this.primaryKeys = new HashSet<>();
    }

    @Override
    public Map<String, Object> generate() {
        return new HashMap<>(Map.of(ESObjectConstants.DSM, new HashMap<>(Map.of(object, transformedList))));
    }

    @Override
    protected void transformObject(Object object) {
        transformedList = Util.transformObjectCollectionToCollectionMap((List) object);
        setPrimaryId();
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
}
