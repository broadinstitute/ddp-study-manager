package org.broadinstitute.dsm.model.elastic.sort;

import org.broadinstitute.dsm.model.elastic.mapping.TypeExtractor;

import java.util.Map;

public class AdditionalValueTypeSort extends Sort {

    AdditionalValueTypeSort(SortBy sortBy, TypeExtractor<Map<String, String>> typeExtractor) {
        super(sortBy, typeExtractor);
    }
}
