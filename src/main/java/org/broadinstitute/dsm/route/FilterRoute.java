package org.broadinstitute.dsm.route;

import com.google.gson.Gson;
import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.broadinstitute.ddp.handlers.util.Result;
import org.broadinstitute.dsm.db.DDPInstance;
import org.broadinstitute.dsm.db.ViewFilter;
import org.broadinstitute.dsm.db.dao.ddp.participant.ParticipantDataDao;
import org.broadinstitute.dsm.db.dto.ddp.participant.ParticipantDataDto;
import org.broadinstitute.dsm.db.structure.DBElement;
import org.broadinstitute.dsm.model.Filter;
import org.broadinstitute.dsm.model.ParticipantWrapper;
import org.broadinstitute.dsm.model.TissueList;
import org.broadinstitute.dsm.model.TissueListWrapper;
import org.broadinstitute.dsm.model.filter.tissue.DefaultFilterTissueList;
import org.broadinstitute.dsm.model.filter.tissue.ManualFilterTissueList;
import org.broadinstitute.dsm.model.filter.tissue.SearchBarFilterTissueList;
import org.broadinstitute.dsm.security.RequestHandler;
import org.broadinstitute.dsm.statics.DBConstants;
import org.broadinstitute.dsm.statics.RequestParameter;
import org.broadinstitute.dsm.statics.RoutePath;
import org.broadinstitute.dsm.statics.UserErrorMessages;
import org.broadinstitute.dsm.util.ElasticSearchUtil;
import org.broadinstitute.dsm.util.ParticipantUtil;
import org.broadinstitute.dsm.util.PatchUtil;
import org.broadinstitute.dsm.util.UserUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.QueryParamsMap;
import spark.Request;
import spark.Response;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class FilterRoute extends RequestHandler {

    public static final String PARENT_PARTICIPANT_LIST = "participantList";
    public static final String TISSUE_LIST_PARENT = "tissueList";
    public static final String PARTICIPANT_DATA = "participantData";
    public static final String OPTIONS = "OPTIONS";
    public static final String RADIO = "RADIO";
    private static final Logger logger = LoggerFactory.getLogger(FilterRoute.class);
    private static final Gson gson = new Gson();
    private static final ParticipantDataDao participantDataDao = new ParticipantDataDao();
    public static final String PARTICIPANTS = "PARTICIPANTS";
    private static Pattern DATE_PATTERN = Pattern.compile(
            "^\\d{4}-\\d{2}-\\d{2}$");

    private PatchUtil patchUtil;

    public FilterRoute(@NonNull PatchUtil patchUtil) {
        this.patchUtil = patchUtil;
    }

    @Override
    public Object processRequest(Request request, Response response, String userId) throws Exception {
        if (patchUtil.getColumnNameMap() == null) {
            throw new RuntimeException("ColumnNameMap is null!");
        }
        QueryParamsMap queryParams = request.queryMap();
        String parent = null;
        if (queryParams.value(DBConstants.FILTER_PARENT) != null) {
            parent = queryParams.get(DBConstants.FILTER_PARENT).value();
        }
        String realm = null;
        DDPInstance instance = null;
        if (queryParams.value(RoutePath.REALM) != null) {
            realm = queryParams.get(RoutePath.REALM).value();
            instance = DDPInstance.getDDPInstanceWithRole(realm, DBConstants.MEDICAL_RECORD_ACTIVATED);
        } else {
            throw new RuntimeException("No realm is sent!");
        }
        if (UserUtil.checkUserAccess(realm, userId, "mr_view") || UserUtil.checkUserAccess(realm, userId, "pt_list_view")) {
            String json = request.body();
            String userIdRequest = null;
            if (queryParams.value(UserUtil.USER_ID) != null) {
                userIdRequest = queryParams.get(UserUtil.USER_ID).value();
                if (!userId.equals(userIdRequest)) {
                    throw new RuntimeException("User id was not equal. User Id in token " + userId + " user Id in request " + userIdRequest);
                }
            }
            if (request.url().contains(RoutePath.APPLY_FILTER)) {
                return applyFilterMethod(queryParams, parent, realm, instance);
            } else if (request.url().contains(RoutePath.GET_PARTICIPANT)) {
                return getParticipantMethod(queryParams, instance);
            } else if (request.url().contains(RoutePath.FILTER_LIST)) {
                return filterListMethod(queryParams, parent, instance, json);
            }
            throw new RuntimeException("Path was not known");
        } else {
            response.status(500);
            return new Result(500, UserErrorMessages.NO_RIGHTS);
        }
    }

    private Object filterListMethod(QueryParamsMap queryParams, String parent, DDPInstance instance,
                                    String json) {
        if (TISSUE_LIST_PARENT.equals(parent)) {
            return new DefaultFilterTissueList(json).filter(queryParams);
        }
        //manual search
        // return pt list as stream // return doFiltering(json, instance, null, parent, null);
        return doFiltering(json, instance, null, parent, null);
    }

    private List<ParticipantWrapper> getParticipantMethod(QueryParamsMap queryParams, DDPInstance instance) {
        String ddpParticipantId = queryParams.get(RoutePath.ddpParticipantId).value();
        if (StringUtils.isBlank(ddpParticipantId)) {
            throw new RuntimeException("No Participant Id was sent");
        }

        Map<String, String> queryConditions = Map.of(
                "p", " AND p.ddp_participant_id = '" + ddpParticipantId + "'",
                "ES", (ParticipantUtil.isGuid(ddpParticipantId) ? ElasticSearchUtil.BY_GUID : ElasticSearchUtil.BY_LEGACY_ALTPID) + ddpParticipantId
        );
        return ParticipantWrapper.getFilteredList(instance, queryConditions);
    }

    private List<?> applyFilterMethod(QueryParamsMap queryParams, String parent, String realm,
                                      DDPInstance instance) {
        if (StringUtils.isBlank(parent)) {
            throw new RuntimeException("No parent was sent in the request.");
        }
        String filterQuery = queryParams.get("filterQuery").value();
        if (filterQuery != null) {
            return new SearchBarFilterTissueList().filter(queryParams);
        } else {
            String filterName = queryParams.get(RequestParameter.FILTER_NAME).value();

            Filter[] filters = null;
            if (PARENT_PARTICIPANT_LIST.equals(parent)) {
                List<?> filteredByQuickOrSaved = filterByQuickElseSaved(queryParams, parent, instance, filterName);
                if (!filteredByQuickOrSaved.isEmpty()) {
                    return filteredByQuickOrSaved;
                }
                return ParticipantWrapper.getFilteredList(instance, null);
            } else { //for tissue list parent !!!
                return new ManualFilterTissueList(null).filter(queryParams);
            }
        }
    }

    private List<?> filterByQuickElseSaved(QueryParamsMap queryParams, String parent, DDPInstance instance, String filterName) {
        List<?> result = new ArrayList<>();
        Filter[] filters = null;
        if (StringUtils.isNotBlank(filterName)) {
            //quick filter name ptL
            ViewFilter requestForFiltering = new ViewFilter(filterName, parent);
            requestForFiltering.setFilterQuery(ViewFilter.getFilterQuery(filterName, parent));
            if (requestForFiltering.getFilters() == null && StringUtils.isNotBlank(requestForFiltering.getFilterQuery())) {
                requestForFiltering = ViewFilter.parseFilteringQuery(requestForFiltering.getFilterQuery(), requestForFiltering);
            }
            filters = requestForFiltering.getFilters();
        } else {
            //saved filter ptL
            if (StringUtils.isNotBlank(queryParams.get(RequestParameter.FILTERS).value())) {
                filters = new Gson().fromJson(queryParams.get(RequestParameter.FILTERS).value(), Filter[].class);
            }
        }
        if (filters != null) {
            //quick and saved filter ptL
            // return pt list as stream // return filterParticipantList(filters, patchUtil.getColumnNameMap(), instance);
            result = filterParticipantList(filters, patchUtil.getColumnNameMap(), instance);
        }
        return result;
    }

    public List<?> doFiltering(String json, DDPInstance instance, String filterName, String parent, Filter[] savedFilters) {
        String filterQuery = "";
        Filter[] filters = null;
        String quickFilterName = "";
        if (json != null) {
            ViewFilter requestForFiltering = new Gson().fromJson(json, ViewFilter.class);
            if (requestForFiltering != null) {
                if (requestForFiltering.getFilters() == null && StringUtils.isNotBlank(requestForFiltering.getFilterQuery())) {
                    filterQuery = ViewFilter.changeFieldsInQuery(requestForFiltering.getFilterQuery(), false);
                    requestForFiltering = ViewFilter.parseFilteringQuery(filterQuery, requestForFiltering);
                }
                filters = requestForFiltering.getFilters();
            }
            quickFilterName = requestForFiltering == null ? null : requestForFiltering.getQuickFilterName();
        } else if (savedFilters != null) {
            filters = savedFilters;
        }

        List<?> data;
        if (PARENT_PARTICIPANT_LIST.equals(parent)) {
            //manual search and search bar ptL
            data = filterParticipantList(filters, patchUtil.getColumnNameMap(), instance);
        } else {
            data = filterTissueList(filters, patchUtil.getColumnNameMap(), filterName == null ? quickFilterName : filterName, instance, filterQuery);
        }
        logger.info("Returning a filtered list with size " + data.size());
        return data;
    }

    public static List<?> filterParticipantList(Filter[] filters, Map<String, DBElement> columnNameMap, @NonNull DDPInstance instance) {
        Map<String, String> queryConditions = new HashMap<>();
        List<ParticipantDataDto> allParticipantData = null;
        if (filters != null && columnNameMap != null && !columnNameMap.isEmpty()) {
            for (Filter filter : filters) {
                if (filter != null) {
                    String tmp = null;
                    if (filter.getParticipantColumn() != null) {
                        tmp = StringUtils.isNotBlank(filter.getParentName()) ? filter.getParentName() : filter.getParticipantColumn().getTableAlias();
                    }
                    String tmpName = null;
                    DBElement dbElement = null;
                    if (filter.getFilter1() != null && StringUtils.isNotBlank(filter.getFilter1().getName())) {
                        tmpName = filter.getFilter1().getName();
                    } else if (filter.getFilter2() != null && StringUtils.isNotBlank(filter.getFilter2().getName())) {
                        tmpName = filter.getFilter2().getName();
                    }
                    if (filter.getParticipantColumn() != null && PARTICIPANT_DATA.equals(filter.getParticipantColumn().tableAlias)) {
                        if (allParticipantData == null) {
                            allParticipantData = participantDataDao
                                    .getParticipantDataByInstanceId(Integer.parseInt(instance.getDdpInstanceId()));
                        }
                        addParticipantDataFilters(queryConditions, filter, tmpName, allParticipantData);
                    } else {
                        if (StringUtils.isNotBlank(tmpName)) {
                            dbElement = columnNameMap.get(tmp + "." + tmpName);
                        }
                        ViewFilter.addQueryCondition(queryConditions, dbElement, filter);
                    }
                }
            }
        }

        if (!queryConditions.isEmpty()) {
            //combine queries
            Map<String, String> mergeConditions = new HashMap<>();
            for (String filter : queryConditions.keySet()) {
                if (DBConstants.DDP_PARTICIPANT_RECORD_ALIAS.equals(filter)
                        || DBConstants.DDP_PARTICIPANT_EXIT_ALIAS.equals(filter) || DBConstants.DDP_ONC_HISTORY_ALIAS.equals(filter)) {
                    mergeConditions.merge(DBConstants.DDP_PARTICIPANT_ALIAS, queryConditions.get(filter), String::concat);
                } else if (DBConstants.DDP_INSTITUTION_ALIAS.equals(filter)) {
                    mergeConditions.merge(DBConstants.DDP_MEDICAL_RECORD_ALIAS, queryConditions.get(filter), String::concat);
                } else if (DBConstants.DDP_TISSUE_ALIAS.equals(filter)) {
                    mergeConditions.merge(DBConstants.DDP_ONC_HISTORY_DETAIL_ALIAS, queryConditions.get(filter), String::concat);
                } else {
                    mergeConditions.merge(filter, queryConditions.get(filter), String::concat);
                }
            }

            logger.info("Found query conditions for " + mergeConditions.size() + " tables");
            //search bar ptL
            return ParticipantWrapper.getFilteredList(instance, mergeConditions);
        } else {
            return ParticipantWrapper.getFilteredList(instance, null);
        }
    }

    public static void addParticipantDataFilters(Map<String, String> queryConditions,
                                                 Filter filter, String fieldName, List<ParticipantDataDto> allParticipantData) {
        Map<String, String> participantIdsForQuery = new HashMap();
        Map<String, String> participantsNotToAdd = new HashMap();
        for (ParticipantDataDto participantData : allParticipantData) {
            String data = participantData.getData().orElse(null);
            String fieldTypeId = participantData.getFieldTypeId().orElse(null);
            if (data == null || fieldTypeId == null) {
                continue;
            }
            String ddpParticipantId = participantData.getDdpParticipantId().orElse(null);
            Map<String, String> dataMap = gson.fromJson(data, Map.class);
            boolean questionWithOptions = (OPTIONS.equals(filter.getType()) || RADIO.equals(filter.getType())) && filter.getSelectedOptions() != null;
            boolean notEmptyCheck = filter.isNotEmpty() && dataMap.get(fieldName) != null && !dataMap.get(fieldName).isEmpty();
            boolean emptyCheck = filter.isEmpty() && (dataMap.get(fieldName) == null || dataMap.get(fieldName).isEmpty());
            if (notEmptyCheck || emptyCheck && !participantsNotToAdd.containsKey(ddpParticipantId)) {
                participantIdsForQuery.put(ddpParticipantId, fieldName);
                continue;
            }
            //For the participants, which are saved in several rows (for example AT participants)
            boolean shouldNotHaveBeenAdded = filter.isEmpty() && !(dataMap.get(fieldName) == null || dataMap.get(fieldName).isEmpty())
                    && !fieldTypeId.contains(PARTICIPANTS);
            if (shouldNotHaveBeenAdded) {
                participantIdsForQuery.remove(ddpParticipantId);
                participantsNotToAdd.put(ddpParticipantId, fieldName);
                continue;
            }

            if (questionWithOptions) {
                for (String option : filter.getSelectedOptions()) {
                    if (dataMap.get(fieldName) != null && dataMap.get(fieldName)
                            .equals(option)) {
                        participantIdsForQuery.put(ddpParticipantId, fieldName);
                        break;
                    }
                }
            } else if (filter.getFilter1() != null && filter.getFilter1().getValue() != null) {
                boolean singleValueMatches;
                if (filter.isExactMatch()) {
                    singleValueMatches = dataMap.get(fieldName) != null && dataMap.get(fieldName)
                            .equals(filter.getFilter1().getValue());
                } else {
                    singleValueMatches = dataMap.get(fieldName) != null && dataMap.get(fieldName).toLowerCase()
                            .contains(filter.getFilter1().getValue().toString().toLowerCase());
                }
                if (singleValueMatches) {
                    participantIdsForQuery.put(ddpParticipantId, fieldName);
                } else if (filter.getFilter2() != null) {
                    addConditionForRange(filter, fieldName, dataMap, participantIdsForQuery, ddpParticipantId);
                }
            }
        }
        if (!participantIdsForQuery.isEmpty()) {
            StringBuilder newCondition = getNewCondition(participantIdsForQuery);
            newCondition.append(ElasticSearchUtil.CLOSING_PARENTHESIS);
            String esCondition = queryConditions.get(ElasticSearchUtil.ES);
            esCondition += newCondition.toString();
            queryConditions.put(ElasticSearchUtil.ES, esCondition);
        } else {
            //so that empty list is returned
            queryConditions.put(ElasticSearchUtil.ES, ElasticSearchUtil.BY_PROFILE_GUID + ElasticSearchUtil.EMPTY);
        }
    }

    public static StringBuilder getNewCondition(Map<String, String> participantIdsForQuery) {
        StringBuilder newCondition = new StringBuilder(ElasticSearchUtil.AND);
        int i = 0;
        for (String id : participantIdsForQuery.keySet()) {
            if (i == 0) {
                newCondition.append(ParticipantUtil.isGuid(id) ? ElasticSearchUtil.BY_PROFILE_GUID + id : ElasticSearchUtil.BY_PROFILE_LEGACY_ALTPID + id);
            } else {
                newCondition.append(ParticipantUtil.isGuid(id) ? ElasticSearchUtil.BY_GUIDS + id : ElasticSearchUtil.BY_LEGACY_ALTPIDS + id);
            }
            i++;
        }
        return newCondition;
    }

    public static void addConditionForRange(Filter filter, String fieldName, Map<String, String> dataMap, Map<String, String> participantIdsForQuery, String ddpParticipantId) {
        Object rangeValue1 = filter.getFilter1().getValue();
        Object rangeValue2 = filter.getFilter2().getValue();
        if (rangeValue1 == null || rangeValue2 == null) {
            return;
        }

        boolean inNumberRange = isInNumberRange(fieldName, dataMap, rangeValue1, rangeValue2);

        boolean inDateRange = isInDateRange(fieldName, dataMap, rangeValue1, rangeValue2);

        if (inNumberRange || inDateRange) {
            participantIdsForQuery.put(ddpParticipantId, fieldName);
        }
    }

    public static boolean isInDateRange(String fieldName, Map<String, String> dataMap, Object rangeValue1, Object rangeValue2) {
        return (dataMap.get(fieldName) != null)
                    && rangeValue1 instanceof String && rangeValue2 instanceof String
                    && DATE_PATTERN.matcher((String) rangeValue1).matches() && DATE_PATTERN.matcher((String) rangeValue2).matches()
                    && LocalDate.parse(dataMap.get(fieldName)).compareTo(LocalDate.parse((String) rangeValue1)) >= 0
                    && ((LocalDate.parse(dataMap.get(fieldName)).compareTo(LocalDate.parse((String) rangeValue1)) >= 0
                        && LocalDate.parse(dataMap.get(fieldName)).compareTo(LocalDate.parse((String) rangeValue2)) < 0)
                        || (LocalDate.parse(dataMap.get(fieldName)).compareTo(LocalDate.parse((String) rangeValue2)) >= 0
                            && LocalDate.parse(dataMap.get(fieldName)).compareTo(LocalDate.parse((String) rangeValue1)) < 0));
    }

    public static boolean isInNumberRange(String fieldName, Map<String, String> dataMap, Object rangeValue1, Object rangeValue2) {
        boolean dataIsNumber = dataMap.get(fieldName) != null && NumberUtils.isNumber(dataMap.get(fieldName));
        boolean moreThanFirstNumber = dataIsNumber && rangeValue1 instanceof Double && Double.compare(Double.parseDouble(dataMap.get(fieldName)), (Double) rangeValue1) >= 0;
        boolean moreThanSecondNumber = dataIsNumber && rangeValue2 instanceof Double && Double.compare(Double.parseDouble(dataMap.get(fieldName)), (Double) rangeValue2) >= 0;
        //range will be starting from the lower number up until the higher number
        return (moreThanFirstNumber && !moreThanSecondNumber) || (moreThanSecondNumber && !moreThanFirstNumber);
    }

    public static List<?> filterTissueList(Filter[] filters, Map<String, DBElement> columnNameMap, String filterName,
                                           @NonNull DDPInstance instance, String filterQuery) {
        Map<String, String> queryConditions = new HashMap<>();
        String subQueryForFiltering = "";
        if (filters != null && !columnNameMap.isEmpty()) {
            for (Filter filter : filters) {
                if (filter != null) {
                    DBElement dbElement = columnNameMap.get(filter.getParticipantColumn().tableAlias + "." + filter.getFilter1().getName());
                    ViewFilter.addQueryCondition(queryConditions, dbElement, filter);
                }
            }
        }

        if (StringUtils.isNotBlank(filterQuery) && StringUtils.isBlank(subQueryForFiltering)) {
            subQueryForFiltering = filterQuery;
        }
        //filter now
        if (StringUtils.isNotBlank(subQueryForFiltering)) {
            String[] queryConditionsFilter = subQueryForFiltering.split("(AND)");
            for (String queryCondition : queryConditionsFilter) {
                if (queryCondition.indexOf(DBConstants.DDP_PARTICIPANT_RECORD_ALIAS + DBConstants.ALIAS_DELIMITER) > 0
                        || queryCondition.indexOf(DBConstants.DDP_ONC_HISTORY_ALIAS + DBConstants.ALIAS_DELIMITER) > 0
                        || queryCondition.indexOf(DBConstants.DDP_INSTITUTION_ALIAS + DBConstants.ALIAS_DELIMITER) > 0
                        || queryCondition.indexOf(DBConstants.DDP_PARTICIPANT_EXIT_ALIAS + DBConstants.ALIAS_DELIMITER) > 0
                        || queryCondition.indexOf(DBConstants.DDP_PARTICIPANT_ALIAS + DBConstants.ALIAS_DELIMITER) > 0
                        || queryCondition.indexOf(DBConstants.DDP_MEDICAL_RECORD_ALIAS + DBConstants.ALIAS_DELIMITER) > 0
                        || queryCondition.indexOf(DBConstants.DDP_KIT_REQUEST_ALIAS + DBConstants.ALIAS_DELIMITER) > 0
                        || queryCondition.indexOf(DBConstants.DDP_ABSTRACTION_ALIAS + DBConstants.ALIAS_DELIMITER) > 0) {
                    throw new RuntimeException(" Filtering for pt or medical record is not allowed in Tissue List");
                } else if (queryCondition.indexOf(DBConstants.DDP_ONC_HISTORY_DETAIL_ALIAS + DBConstants.ALIAS_DELIMITER) > 0) {
                    queryConditions.put(DBConstants.DDP_ONC_HISTORY_DETAIL_ALIAS, queryCondition);
                } else if (queryCondition.indexOf(DBConstants.DDP_TISSUE_ALIAS + DBConstants.ALIAS_DELIMITER) > 0) {
                    queryConditions.put(DBConstants.DDP_TISSUE_ALIAS, queryCondition);
                } else {
                    queryConditions.put("ES", queryCondition);
                }
            }
        }

        if (!queryConditions.isEmpty()) {
            logger.info("Found query conditions for " + queryConditions.size() + " tables");
        }
        String queryString = "";
        for (String key : queryConditions.keySet()) {
            if (!"ES".equals(key)) {
                queryString += queryConditions.get(key);
            }
        }
        queryString += subQueryForFiltering;
        return getListBasedOnFilterName(filterName, instance.getName(), TISSUE_LIST_PARENT, queryString, queryConditions);
    }

    public static List<?> getListBasedOnFilterName(String filterName, String realm, String parent, String queryString, Map<String, String> filters) {
        List<TissueListWrapper> wrapperList = new ArrayList<>();
        if (TISSUE_LIST_PARENT.equals(parent)) {
            DDPInstance instance = DDPInstance.getDDPInstanceWithRole(realm, DBConstants.MEDICAL_RECORD_ACTIVATED);
            String subQueryForFiltering = "";
            if (StringUtils.isNotBlank(filterName)) {
                if (filterName.equals(ViewFilter.DESTROYING_FILTERS)) {
                    List<TissueList> tissueLists = ViewFilter.getDestroyingSamples(instance.getName());
                    wrapperList = TissueListWrapper.getTissueListData(instance, filters, tissueLists);
                    return wrapperList;
                }
                subQueryForFiltering = ViewFilter.getFilterQuery(filterName, parent);
            }
            String[] tableFilters = subQueryForFiltering.split("(AND)");
            String query = " ";
            for (String filter : tableFilters) {
                if (StringUtils.isNotBlank(filter)) {
                    if (!filter.contains(ElasticSearchUtil.PROFILE + DBConstants.ALIAS_DELIMITER) && !filter.contains(ElasticSearchUtil.DATA + DBConstants.ALIAS_DELIMITER)) {
                        query += "AND " + filter + " ";
                    } else {
                        if (filters == null) {
                            filters = new HashMap<>();
                        }
                        String q = filters.getOrDefault("ES", " ");
                        filters.put("ES", q + " " + filter);
                    }
                }
            }
            List<TissueList> tissueLists = TissueList.getAllTissueListsForRealm(realm, TissueList.SQL_SELECT_ALL_ONC_HISTORY_TISSUE_FOR_REALM + (queryString != null ? queryString : "") + query);
            wrapperList = TissueListWrapper.getTissueListData(instance, filters, tissueLists);
            return wrapperList;
        }
        return wrapperList;
    }

    private void streamResponse(@NonNull Response response, List<?> result) throws IOException {
        OutputStreamWriter writer = new OutputStreamWriter(response.raw().getOutputStream());
        new Gson().toJson(result, writer);
        writer.flush();
        writer.close();
    }
}
