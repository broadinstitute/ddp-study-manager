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
    }
}
