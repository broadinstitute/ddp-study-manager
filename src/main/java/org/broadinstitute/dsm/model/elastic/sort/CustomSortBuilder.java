package org.broadinstitute.dsm.model.elastic.sort;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.dsm.statics.DBConstants;
import org.broadinstitute.dsm.util.ElasticSearchUtil;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.NestedSortBuilder;

public class CustomSortBuilder extends FieldSortBuilder {



    public CustomSortBuilder(SortBy sortBy) {
        super("dsm.kitRequestShipping.testResult.isCorrected");
        setNestedSort(new NestedSortBuilder("dsm.kitRequestShipping.testResult"));
    }


    static String buildFieldName(SortBy sortBy) {
        Alias alias = Alias.of(sortBy.getTableAlias());
        Type type = Type.of(sortBy);
        return Stream.of(ElasticSearchUtil.DSM, alias.getValue(), type.getValue(), sortBy.getInnerProperty())
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.joining(DBConstants.ALIAS_DELIMITER));
    }
}
