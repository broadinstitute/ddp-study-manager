package org.broadinstitute.dsm.model.elastic.export;

import java.util.Map;

public interface Exportable {
    void export(Map<String, Object> source);
}
