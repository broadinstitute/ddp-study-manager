package org.broadinstitute.dsm.model.elastic.filter;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.dsm.util.ElasticSearchUtil;

public abstract class BaseSplitter {

    protected String filter;
    protected String[] splittedFilter;

    public abstract String[] split();

    public String[] getValue() {
        if (splittedFilter.length > 1) {
            return new String[]{splittedFilter[1].trim()};
        }
        return new String[]{StringUtils.EMPTY};
    }

    public String getAlias() {
        return getFieldWithAlias()[0];
    }

    public String getInnerProperty() {
        return getFieldWithAlias()[1];
    }

    protected String[] getFieldWithAlias() {
        return splittedFilter[0].trim().split(ElasticSearchUtil.DOT_SEPARATOR);
    }

    public void setFilter(String filter) {
        this.filter = filter;
        splittedFilter = split();
    }
}
