package org.broadinstitute.dsm.model.elastic.filter;

import org.elasticsearch.index.query.AbstractQueryBuilder;

public abstract class DsmAbstractQueryBuilder {

    protected String filter;

    public DsmAbstractQueryBuilder(String filter) {
        this.filter = filter;
    }

    public abstract AbstractQueryBuilder build();
}
