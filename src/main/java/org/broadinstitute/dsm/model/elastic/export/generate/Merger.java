package org.broadinstitute.dsm.model.elastic.export.generate;

import java.util.Map;

public interface Merger {

    Map<String, Object> merge(Map<String, Object> base, Map<String, Object> toMerge);

}
