package org.broadinstitute.dsm.model.elastic.sort;

import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.NestedSortBuilder;

public class CustomSortBuilder extends FieldSortBuilder {

    public CustomSortBuilder(Sort sort) {
        super(sort.buildFieldName());
        if (sort.isNestedSort())
            setNestedSort(new NestedSortBuilder(sort.buildNestedPath()));
        this.order(sort.getOrder());
    }

}


