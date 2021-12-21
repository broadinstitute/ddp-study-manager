package org.broadinstitute.dsm.model.elastic.filter;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.dsm.model.Filter;
import org.broadinstitute.dsm.statics.ESObjectConstants;
import org.broadinstitute.dsm.util.ElasticSearchUtil;

public class JsonExtractSplitter extends EqualsSplitter {

    @Override
    public String[] getValue() {
        return new String[]{splittedFilter[1].trim()};
    }

    @Override
    public String getInnerProperty() {
        return ESObjectConstants.DYNAMIC_FIELDS;
    }

    @Override
    protected String[] getFieldWithAlias() {
        return splittedFilter[0]
                .split(Filter.JSON_EXTRACT)[1]
                .replace(Filter.OPEN_PARENTHESIS, StringUtils.EMPTY)
                .replace(Filter.CLOSE_PARENTHESIS, StringUtils.EMPTY)
                .trim()
                .split(",")[0]
                .split(ElasticSearchUtil.DOT_SEPARATOR);
    }
}
