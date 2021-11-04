package org.broadinstitute.dsm.model.elastic.migrationscript;

import java.util.HashMap;
import java.util.Map;

public class MapAdapter {

    public static <T, V> Map<T, V> of(Map<T,V>... maps) {
        Map<T, V> result = new HashMap<>();
        for (Map<T, V> map: maps) {
            result.putAll(map);
        }
        return result;
    }

}
