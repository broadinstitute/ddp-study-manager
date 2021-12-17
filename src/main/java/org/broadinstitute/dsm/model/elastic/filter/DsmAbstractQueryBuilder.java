package org.broadinstitute.dsm.model.elastic.filter;

import org.broadinstitute.dsm.model.elastic.export.parse.Parser;
import org.broadinstitute.dsm.statics.DBConstants;
import org.broadinstitute.dsm.statics.ESObjectConstants;
import org.elasticsearch.index.query.AbstractQueryBuilder;

public abstract class DsmAbstractQueryBuilder {

    protected static final String DSM_WITH_DOT = ESObjectConstants.DSM + DBConstants.ALIAS_DELIMITER;
    protected String filter;
    protected Parser parser;

    public DsmAbstractQueryBuilder(String filter, Parser parser) {
        this.parser = parser;
        this.filter = filter;
    }

    public DsmAbstractQueryBuilder() {}

    public void setFilter(String filter) {
        this.filter = filter;
    }

    public void setParser(Parser parser) {
        this.parser = parser;
    }

    public abstract AbstractQueryBuilder build();
}
