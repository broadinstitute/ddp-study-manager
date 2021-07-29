package org.broadinstitute.dsm.model.filter.tissue;

import java.util.List;

import org.broadinstitute.dsm.model.TissueListWrapper;
import org.broadinstitute.dsm.model.filter.Filterable;
import spark.QueryParamsMap;

public class FilterTissueList {

    private Filterable<TissueListWrapper> tissueListWrapperFilterable;

    public FilterTissueList(Filterable<TissueListWrapper> filterable) {
        tissueListWrapperFilterable = filterable;
    }

    public void changeFilterStrategy(Filterable<TissueListWrapper> filterable) {
        tissueListWrapperFilterable = filterable;
    }

    public List<TissueListWrapper> filter(QueryParamsMap queryParamsMap) {
        return tissueListWrapperFilterable.filter(queryParamsMap);
    }

}
