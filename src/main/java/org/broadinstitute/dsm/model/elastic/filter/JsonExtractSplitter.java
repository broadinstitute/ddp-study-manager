package org.broadinstitute.dsm.model.elastic.filter;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.dsm.model.Filter;
import org.broadinstitute.dsm.model.elastic.Util;
import org.broadinstitute.dsm.statics.ESObjectConstants;
import org.broadinstitute.dsm.util.ElasticSearchUtil;

public class JsonExtractSplitter extends BaseSplitter {

    private BaseSplitter decoratedSplitter;

    public JsonExtractSplitter(BaseSplitter splitter) {
        this.decoratedSplitter = splitter;
    }

    public JsonExtractSplitter() {
        decoratedSplitter = new EqualsSplitter();
    }

    @Override
    public String[] split() {
        decoratedSplitter.filter = filter;
        return decoratedSplitter.split();
    }

    @Override
    protected String[] getFieldWithAlias() {
        String[] splittedByJsonExtractAndComma = splittedFilter[0]
                .split(Filter.JSON_EXTRACT)[1]
                .replace(Filter.OPEN_PARENTHESIS, StringUtils.EMPTY)
                .replace(Filter.CLOSE_PARENTHESIS, StringUtils.EMPTY)
                .trim()
                .split(",");
        String[] splittedByDot = splittedByJsonExtractAndComma[0].split(ElasticSearchUtil.DOT_SEPARATOR);
        String alias = splittedByDot[0];
        String removedSingleQuotes = splittedByJsonExtractAndComma[1]
                .replace("'", StringUtils.EMPTY)
                .trim();
        String innerProperty = removedSingleQuotes
                .substring(removedSingleQuotes.indexOf(".")+1);
        return new String[] {alias, String.join(".", ESObjectConstants.DYNAMIC_FIELDS,
                innerProperty)};
    }
}
