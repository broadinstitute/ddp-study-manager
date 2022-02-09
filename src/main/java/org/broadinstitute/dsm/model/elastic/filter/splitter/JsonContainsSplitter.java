package org.broadinstitute.dsm.model.elastic.filter.splitter;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.dsm.model.Filter;
import org.broadinstitute.dsm.model.elastic.Util;
import org.broadinstitute.dsm.statics.DBConstants;
import org.broadinstitute.dsm.statics.ESObjectConstants;
import org.broadinstitute.dsm.util.ElasticSearchUtil;

public class JsonContainsSplitter extends BaseSplitter {

    @Override
    public String[] split() {
        return filter.split(Filter.JSON_CONTAINS)[1]
                .replace(Filter.OPEN_PARENTHESIS, StringUtils.EMPTY)
                .replace(Filter.CLOSE_PARENTHESIS,StringUtils.EMPTY)
                .split(Filter.JSON_OBJECT);

    }

    @Override
    public String[] getValue() {
        return new String[] {splittedFilter[1].split(Util.COMMA_SEPARATOR)[1].trim()};
    }

    @Override
    public String getInnerProperty() {
        String nestedProperty = splittedFilter[1]
                .split(Util.COMMA_SEPARATOR)[0]
                .replaceAll(Filter.SINGLE_QUOTE, StringUtils.EMPTY);
        return String.join(DBConstants.ALIAS_DELIMITER, Util.underscoresToCamelCase(getFieldWithAlias()[1]), nestedProperty);
    }

    @Override
    protected String[] getFieldWithAlias() {
        return splittedFilter[0]
                .replace(Util.COMMA_SEPARATOR, StringUtils.EMPTY)
                .trim()
                .split(ElasticSearchUtil.ESCAPE_CHARACTER_DOT_SEPARATOR);
    }
}
