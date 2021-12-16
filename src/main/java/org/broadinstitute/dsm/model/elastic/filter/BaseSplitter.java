package org.broadinstitute.dsm.model.elastic.filter;

import org.broadinstitute.dsm.util.ElasticSearchUtil;

public abstract class BaseSplitter {

    protected String filter;
    protected String[] splittedFilter;

    public abstract String[] split();

    public String getValue() {
        return splittedFilter[1].trim();
    }

    public String getAlias() {
        return getFieldWithAlias()[0];
    }

    public String getInnerProperty() {
        return getFieldWithAlias()[1];
    }

    private String[] getFieldWithAlias() {
        return splittedFilter[0].trim().split(ElasticSearchUtil.DOT_SEPARATOR);
    }

    public void setFilter(String filter) {
        this.filter = filter;
        splittedFilter = split();
    }
}
