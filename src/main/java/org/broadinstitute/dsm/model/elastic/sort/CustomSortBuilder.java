package org.broadinstitute.dsm.model.elastic.sort;

import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.dsm.model.Filter;
import org.broadinstitute.dsm.statics.DBConstants;
import org.broadinstitute.dsm.statics.ESObjectConstants;
import org.broadinstitute.dsm.util.ElasticSearchUtil;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.NestedSortBuilder;

public class CustomSortBuilder extends FieldSortBuilder {

    public CustomSortBuilder(Filter sortBy) {
        super("");
//        setNestedSort();
    }


    static String buildFieldName(Filter sortBy) {
        String tableAlias = sortBy.getParticipantColumn().getTableAlias();
        //if (StringUtils.isBlank(tableAlias)) return StringUtils.EMPTY;
        //tableAlias -> data,m,p,oD...
        //dsm.profile(object).hruid(name)
        String fieldName = ESObjectConstants.DSM + DBConstants.ALIAS_DELIMITER;
        switch (tableAlias) {
            case DBConstants.DDP_ONC_HISTORY_DETAIL_ALIAS:
                fieldName += "oncHistoryDetail";
                break;
            default:
                fieldName = ElasticSearchUtil.PROFILE_CREATED_AT;
                break;
        }
        return null;
    }
}
