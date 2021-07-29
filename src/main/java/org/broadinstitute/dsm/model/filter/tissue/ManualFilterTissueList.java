package org.broadinstitute.dsm.model.filter.tissue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;

import com.google.gson.Gson;
import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.dsm.db.DDPInstance;
import org.broadinstitute.dsm.db.ViewFilter;
import org.broadinstitute.dsm.db.structure.DBElement;
import org.broadinstitute.dsm.model.Filter;
import org.broadinstitute.dsm.model.TissueListWrapper;
import org.broadinstitute.dsm.statics.DBConstants;
import org.broadinstitute.dsm.statics.RequestParameter;
import org.broadinstitute.dsm.statics.RoutePath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.QueryParamsMap;

public class ManualFilterTissueList extends BaseFilterTissueList {

    private static final Logger logger = LoggerFactory.getLogger(ManualFilterTissueList.class);


    public ManualFilterTissueList(String jsonMap) {
        super(jsonMap);
    }

    @Override
    public List<TissueListWrapper> filter(QueryParamsMap queryParamsMap) {
        prepareNeccesaryData(jsonBody, queryParamsMap);
        String filterName = Objects.requireNonNull(queryParamsMap).get(RequestParameter.FILTER_NAME).value();
        if (!queryParamsMap.hasKey(RoutePath.REALM)) throw new NoSuchElementException("realm is necessary");
        String realm = queryParamsMap.get(RoutePath.REALM).value();
        DDPInstance ddpInstance = DDPInstance.getDDPInstance(realm);
        if (!queryParamsMap.hasKey(DBConstants.FILTER_PARENT)) throw new RuntimeException("parent is necessary");
        Filter[] filters = null;
        if (StringUtils.isBlank(filterName)) {
            filters = new Gson().fromJson(queryParamsMap.get(RequestParameter.FILTERS).value(), Filter[].class);
        }
        return filterTissueList(filters, patchUtil.getColumnNameMap(), filterName == null ? quickFilterName : filterName, ddpInstance, filterQuery);

    }




}
