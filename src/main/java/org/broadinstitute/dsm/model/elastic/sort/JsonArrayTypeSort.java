package org.broadinstitute.dsm.model.elastic.sort;

import org.broadinstitute.dsm.model.elastic.mapping.TypeExtractor;

import java.util.Map;

public class JsonArrayTypeSort extends Sort {

    JsonArrayTypeSort(SortBy sortBy, TypeExtractor<Map<String, String>> typeExtractor) {
        super(sortBy, typeExtractor);
    }

    @Override
    protected String getKeywordIfText(Type innerType) {
        return super.getKeywordIfText(Type.valueOf(sortBy.getAdditionalType()));
    }
}
