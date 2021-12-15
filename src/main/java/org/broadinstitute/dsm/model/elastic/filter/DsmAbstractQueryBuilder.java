package org.broadinstitute.dsm.model.elastic.filter;

import org.broadinstitute.dsm.statics.DBConstants;
import org.broadinstitute.dsm.statics.ESObjectConstants;
import org.elasticsearch.index.query.AbstractQueryBuilder;

public abstract class DsmAbstractQueryBuilder {

    protected static final String DSM_WITH_DOT = ESObjectConstants.DSM + DBConstants.ALIAS_DELIMITER;
    protected String filter;

    public DsmAbstractQueryBuilder(String filter) {
        this.filter = filter;
    }

    public abstract AbstractQueryBuilder build();
}
