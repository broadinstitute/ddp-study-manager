package org.broadinstitute.dsm.model.elastic.sort;

import org.elasticsearch.search.sort.FieldSortBuilder;

import java.util.logging.Filter;

public class CustomSortBuilder extends FieldSortBuilder {

    public CustomSortBuilder(Filter sortBy) {
        super("");
        setNestedSort();
    }


}
