package org.broadinstitute.dsm.db;

import com.google.gson.Gson;
import lombok.Data;
import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.ddp.db.SimpleResult;
import org.broadinstitute.ddp.handlers.util.Result;
import org.broadinstitute.dsm.db.structure.ColumnName;
import org.broadinstitute.dsm.db.structure.DBElement;
import org.broadinstitute.dsm.db.structure.TableName;
import org.broadinstitute.dsm.exception.DuplicateException;
import org.broadinstitute.dsm.model.*;
import org.broadinstitute.dsm.statics.DBConstants;
import org.broadinstitute.dsm.util.PatchUtil;
import org.broadinstitute.dsm.util.SystemUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static org.broadinstitute.ddp.db.TransactionWrapper.inTransaction;

@Data
@TableName (
        name = DBConstants.VIEW_FILTERS,
        alias = "",
        primaryKey = DBConstants.FILTER_ID,
        columnPrefix = "")
public class ViewFilter {
    public static final Logger logger = LoggerFactory.getLogger(ViewFilter.class);

    public static final String SQL_INSERT_VIEW = "INSERT INTO view_filters (view_columns, display_name, created_by, shared, query_items, parent,  quick_filter_name, ddp_group_id, changed_by, last_changed, deleted) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    public static final String SQL_CHECK_VIEW_NAME = "SELECT * FROM view_filters WHERE (display_name = ? ) and deleted <=> 0 ";
    public static final String SQL_SELECT_USER_FILTERS = "SELECT * FROM view_filters WHERE (created_by = ? OR (created_by = 'System' AND ddp_group_id = ? )" +
            "OR (shared = 1 AND ddp_group_id = ? ) OR (ddp_group_id is NULL AND ddp_realm_id LIKE '%#%') ) AND deleted <> 1 ";
    public static final String SQL_SELECT_QUERY_ITEMS = "SELECT query_items, quick_filter_name FROM view_filters WHERE display_name = ? AND parent = ?";
    public static final String SQL_GET_DEFAULT_FILTER = "SELECT display_name from view_filters WHERE default_users LIKE '%#%' AND parent = ?";
    public static final String SQL_SELECT_DEFAULT_FILTER_USERS = "SELECT default_users FROM view_filters WHERE display_name = ? AND parent = ? ";
    public static final String SQL_UPDATE_DEFAULT_FILTER = "UPDATE view_filters SET default_users = ? WHERE display_name = ? AND parent = ? ";
    public static final String SQL_AND_PARENT = " AND parent = ? ";

    public static final String DESTROYING_FILTERS = "destruction";
    public static final String QUERIES_FOR_DIFFERENT_TABLES_DELIMITER = "";

    public Filter filter;

    @ColumnName (DBConstants.DISPLAY_NAME_FILTER)
    private String filterName;

    private Map<String, List<String>> columns;

    private String[] columnsToSave;

    @ColumnName (DBConstants.FILTER_ID)
    private String id;


    @ColumnName (DBConstants.FILTER_DELETED)
    private String fDeleted;

    @ColumnName (DBConstants.SHARED_FILTER)
    private String shared;

    @ColumnName (DBConstants.QUERY_ITEMS)
    private String queryItems;

    @ColumnName (DBConstants.FILTER_ICON)
    public String icon;

    @ColumnName (DBConstants.QUICK_FILTER_NAME)
    public String quickFilterName;

    @ColumnName (DBConstants.FILTER_REALM_ID)
    private Integer[] realmId;

    private String userId;
    public Filter[] filters;
    public String parent;
    public String filterQuery;

    public ViewFilter(String filterName, String parent) {
        this(filterName, null, null, null, null, null, null, parent, null,
                null, null, null, null);
    }

    public ViewFilter(String filterName, Map<String, List<String>> columns,
                      String id, String fDeleted, String shared, String userId, Filter[] filters, String parent, String icon, String quickFilterName, String queryItems, String filterQuery, Integer[] realmId) {
        this.userId = userId;
        this.fDeleted = fDeleted;
        this.filterName = filterName;
        this.columns = columns;
        this.shared = shared;
        this.id = id;
        this.filters = filters;
        this.parent = parent;
        this.icon = icon;
        this.quickFilterName = quickFilterName;
        this.queryItems = queryItems;
        this.filterQuery = filterQuery;
        this.realmId = realmId;
    }

    public static Object saveFilter(@NonNull String filterViewToSave, String userId,
                                    @NonNull Map<String, DBElement> columnNameMap, @NonNull String ddpGroupId) {
        ViewFilter viewFilter = new Gson().fromJson(filterViewToSave, ViewFilter.class);

        String query;
        String suggestedName = viewFilter.getFilterName();
        SimpleResult results = inTransaction((conn) -> {
            SimpleResult dbVals = new SimpleResult();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_CHECK_VIEW_NAME)) {
                stmt.setString(1, suggestedName);
                try {
                    ResultSet rs = stmt.executeQuery();
                    if (rs.next()) {
                        dbVals.resultException = new DuplicateException(viewFilter.getFilterName());
                    }
                }
                catch (Exception e) {
                    dbVals.resultException = e;
                }
            }
            catch (SQLException ex) {
                dbVals.resultException = ex;
            }
            return dbVals;
        });
        if (results.resultException != null) {
            if (results.resultException instanceof SQLIntegrityConstraintViolationException) {
                return new Result(500, "Duplicate Name");
            }
            else {
                throw new RuntimeException(results.resultException);
            }
        }
        if (StringUtils.isBlank(viewFilter.getQueryItems())) {
            Filter[] filters = viewFilter.getFilters();
            Map<String, String> queryConditions = new HashMap<>();
            if (filters != null) {
                if (!columnNameMap.isEmpty()) {
                    for (Filter filter : filters) {
                        if (filter != null) {
                            String parent = filter.getParentName();
                            DBElement dbElement = columnNameMap.get(parent + DBConstants.ALIAS_DELIMITER + filter.getFilter1().getName());
                            addQueryCondition(queryConditions, dbElement, filter);
                        }
                    }
                }
                else {
                    throw new RuntimeException("ColumnNameMap was empty when saving a new filter to the DB.");
                }
            }

            String newQueryCondition = "";
            if (!queryConditions.isEmpty()) {
                if (queryConditions.size() == 1) {
                    for (String key : queryConditions.keySet()) {
                        newQueryCondition = queryConditions.get(key);
                    }
                }
                else {
                    for (String key : queryConditions.keySet()) {
                        newQueryCondition = newQueryCondition + queryConditions.get(key);
                    }
                }
            }
            //            query = newQueryCondition.replaceAll(QUERIES_FOR_DIFFERENT_TABLES_DELIMITER + "$", "").trim();
            query = newQueryCondition;
        }
        else {
            query = changeFieldsInQuery(viewFilter.getQueryItems());
        }
        StringBuilder columnsString = new StringBuilder();
        if (viewFilter.getColumnsToSave() != null) {
            for (String col : viewFilter.getColumnsToSave()) {
                columnsString.append(col + ",");
            }
        }
        return saveFilter(viewFilter, query, userId, ddpGroupId, columnsString.toString());
    }

    private static Result saveFilter(@NonNull ViewFilter viewFilter, @NonNull String query, @NonNull String userId,
                                     @NonNull String ddpGroupId, @NonNull String columnString) {
        SimpleResult results = inTransaction((conn) -> {
            SimpleResult dbVals = new SimpleResult();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_INSERT_VIEW)) {
                stmt.setString(1, columnString);
                stmt.setString(2, viewFilter.getFilterName());
                stmt.setString(3, userId);
                stmt.setString(4, viewFilter.getShared());
                stmt.setString(5, query);
                stmt.setString(6, viewFilter.getParent());
                stmt.setString(7, viewFilter.getQuickFilterName());
                stmt.setString(8, ddpGroupId);
                stmt.setString(9, userId);
                stmt.setLong(10, System.currentTimeMillis());
                stmt.setString(11, viewFilter.getFDeleted());
                try {
                    int result = stmt.executeUpdate();
                    if (result == 1) {
                        try (ResultSet rs = stmt.getGeneratedKeys()) {
                            if (rs.next()) {
                                String id = rs.getString(1);
                                logger.info("Inserted new filter with id " + id);
                            }
                        }
                        catch (Exception e) {
                            dbVals.resultException = e;
                        }
                    }
                }
                catch (Exception e) {
                    dbVals.resultException = e;
                }
            }
            catch (SQLIntegrityConstraintViolationException ex) {
                dbVals.resultException = new DuplicateException(viewFilter.getFilterName());
            }
            catch (SQLException ex) {
                dbVals.resultException = ex;
            }
            return dbVals;
        });
        if (results.resultException != null) {
            throw new RuntimeException(results.resultException);
        }
        return new Result(200);
    }

    public static void addQueryCondition(@NonNull Map<String, String> queryConditions, DBElement dbElement, Filter filter) {
        if (dbElement != null) {
            String queryCondition = "";
            String tmp = StringUtils.isNotBlank(filter.getParentName()) ? filter.getParentName() : filter.getParticipantColumn().getTableAlias();
            if (queryConditions.containsKey(tmp)) {
                queryCondition = queryConditions.get(tmp);
            }
            queryConditions.put(tmp, queryCondition.concat(Filter.getQueryStringForFiltering(filter, dbElement)));
        }
        else {
            String queryCondition = "";
            if (queryConditions.containsKey("ES")) {
                queryCondition = queryConditions.get("ES");
            }
            queryConditions.put("ES", queryCondition.concat(Filter.getQueryStringForFiltering(filter, null)));
        }
    }

    public static List<ViewFilter> getAllFilters(@NonNull String userId,
                                                 @NonNull Map<String, DBElement> columnNameMap,
                                                 String parent, @NonNull String ddpGroupId, String realm) {
        List<ViewFilter> viewFilterList = new ArrayList<>();
        SimpleResult results = inTransaction((conn) -> {
            SimpleResult dbVals = new SimpleResult();
            String query = SQL_SELECT_USER_FILTERS;
            if (StringUtils.isNotBlank(parent)) {
                query = query + SQL_AND_PARENT;
            }
            query = query.replaceAll("(%#%)", "%" + realm + ",%");

            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, userId);
                stmt.setString(2, ddpGroupId);
                stmt.setString(3, ddpGroupId);
                if (StringUtils.isNotBlank(parent)) {
                    stmt.setString(4, parent);
                }
                try {
                    logger.info(stmt.toString());
                    ResultSet rs = stmt.executeQuery();
                    while (rs.next()) {
                        viewFilterList.add(getFilterView(rs, columnNameMap));
                    }
                }
                catch (Exception e) {
                    dbVals.resultException = e;
                }
            }
            catch (SQLException ex) {
                dbVals.resultException = ex;
            }
            return dbVals;
        });
        if (results.resultException != null) {
            throw new RuntimeException(results.resultException);
        }
        logger.info("Found " + viewFilterList.size() + " saved filters");
        return viewFilterList;
    }

    private static ViewFilter getFilterView(@NonNull ResultSet rs, @NonNull Map<String, DBElement> columnNameMap) throws SQLException {
        String[] viewColumns = rs.getString(DBConstants.VIEW_COLUMNS).split(",");
        Map<String, List<String>> columnMap = new HashMap<>();
        for (String column : viewColumns) {
            if (StringUtils.isNotBlank(column)) {
                DBElement dbElement = columnNameMap.get(column);
                if (dbElement != null) {
                    List<String> columnList = new ArrayList<>();
                    if (columnMap.containsKey(dbElement.getTableAlias())) {
                        columnList = columnMap.get(dbElement.getTableAlias());
                    }
                    columnList.add(column.substring(column.lastIndexOf(DBConstants.ALIAS_DELIMITER) + 1));
                    columnMap.put(dbElement.getTableAlias(), columnList);
                }
                else {
                    List<String> columnList = new ArrayList<>();
                    if (columnMap.containsKey("ES")) {
                        columnList = columnMap.get("ES");
                    }
                    columnList.add(column);
                    columnMap.put("ES", columnList);
                }
            }
        }
        ViewFilter filter = new ViewFilter(
                rs.getString(DBConstants.DISPLAY_NAME_FILTER),
                columnMap,
                rs.getString(DBConstants.FILTER_ID),
                rs.getString(DBConstants.DELETED),
                rs.getString(DBConstants.SHARED_FILTER),
                rs.getString(DBConstants.CREATED_BY),
                null,
                rs.getString(DBConstants.FILTER_PARENT),
                rs.getString(DBConstants.FILTER_ICON),
                rs.getString(DBConstants.QUICK_FILTER_NAME),
                parseToFrontEndQuery(rs.getString(DBConstants.QUERY_ITEMS)),
                null,
                new Gson().fromJson(rs.getString(DBConstants.FILTER_REALM_ID), Integer[].class));
        if (!rs.getString(DBConstants.CREATED_BY).contains("System")) {
            String queryToParse = rs.getString(DBConstants.QUERY_ITEMS);
            try {
                filter = parseFilteringQuery(queryToParse, filter);
            }
            catch (Exception e) {
                return filter;

            }
        }
        return filter;
    }

    public String getQuickFilterQuery() {
        return getFilterQuery(this.quickFilterName, this.parent);
    }


    public static List<TissueList> getDestroyingSamples(String realm) {
        String query = " AND oD.request <> 'received' AND oD.request <> 'no' AND oD.request <> 'hold' AND oD.request <> 'returned' AND oD.date_px IS NOT NULL AND oD.destruction_policy IS NOT NULL " +
                "AND oD.destruction_policy <> 'indefinitely' AND oD.destruction_policy <> ''";
        List<TissueList> views = TissueList.getAllTissueListsForRealm(realm, TissueList.SQL_SELECT_ALL_ONC_HISTORY_TISSUE_FOR_REALM + query);
        List<TissueList> results = new ArrayList<>(views.size());
        loop:
        for (TissueList tissueList : views) {
            String dateString = tissueList.getOncHistoryDetails().getDatePX();
            int len = dateString.length();
            int destructionPolicy = 1000;
            if (!tissueList.getOncHistoryDetails().getDestructionPolicy().equals("indefinitely")) {
                destructionPolicy = Integer.parseInt(tissueList.getOncHistoryDetails().getDestructionPolicy());
            }
            SimpleDateFormat sdf = null;
            switch (len) {
                case 4: {
                    sdf = new SimpleDateFormat("yyyy");
                    break;
                }

                case 7: {
                    sdf = new SimpleDateFormat("yyyy-MM");
                    break;
                }

                case 10: {
                    sdf = new SimpleDateFormat("yyyy-MM-dd");
                    break;
                }
            }
            if (sdf == null) {
                logger.warn("The date px " + dateString + " has an unknown format!");
                continue loop;
            }
            long datePx = SystemUtil.getLong(dateString, sdf);
            long lastDay = datePx + destructionPolicy * (SystemUtil.MILLIS_PER_DAY * 365);
            long current = System.currentTimeMillis();
            int days = (int) ((lastDay - current) / SystemUtil.MILLIS_PER_DAY);
            if (days > 0 && days < 181) {
                results.add(tissueList);
            }
        }
        return results;
    }

    public static List<String> getQueryItems(@NonNull String filterName, String parent) {
        ArrayList<String> queryItems = new ArrayList<>();
        SimpleResult results = inTransaction((conn) -> {
            SimpleResult dbVals = new SimpleResult();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_SELECT_QUERY_ITEMS)) {
                stmt.setString(1, filterName);
                stmt.setString(2, parent);
                try {
                    ResultSet rs = stmt.executeQuery();
                    if (rs.next()) {
                        dbVals.resultValue = new ArrayList<String>();
                        queryItems.add(rs.getString(DBConstants.QUERY_ITEMS));
                        queryItems.add(rs.getString(DBConstants.QUICK_FILTER_NAME));
                    }
                }
                catch (Exception e) {
                    dbVals.resultException = e;
                }
            }
            catch (SQLException ex) {
                dbVals.resultException = ex;
            }
            return dbVals;
        });
        if (results.resultException != null) {
            throw new RuntimeException(results.resultException);
        }
        return queryItems;
    }

    public static String getFilterQuery(@NonNull String filterName, @NonNull String parent) {
        List<String> selectedFilter = getQueryItems(filterName, parent);
        String query = selectedFilter.get(0);
        String quickFilterName = "";
        if (selectedFilter.size() > 1) {
            quickFilterName = selectedFilter.get(1);
        }
        String quickFilterQuery = "";
        if (StringUtils.isNotBlank(quickFilterName)) {
            List<String> basedQuickFilter = getQueryItems(quickFilterName, parent);
            quickFilterQuery = basedQuickFilter.get(0);
        }
        String[] queryWords = query.split("\\s+");
        for (int i = 0; i < queryWords.length; i++) {
            if (StringUtils.isNotBlank(queryWords[i]) && queryWords[i].equals("today")) {
                String date = getDate();
                System.out.println(getDate());

                if (i < queryWords.length - 1 && (queryWords[i + 1].equals("+") || queryWords[i + 1].equals("-"))) {
                    String days = queryWords[i + 2];
                    days = days.replace("d", "");
                    int numberOfDays = Integer.parseInt(days);
                    if (queryWords[i + 1].equals("-")) {
                        numberOfDays *= -1;
                    }
                    LocalDateTime today = LocalDateTime.now();     //Today
                    LocalDateTime tomorrow = today.plusDays(numberOfDays);
                    date = getDate(tomorrow);
                    queryWords[i + 1] = "";
                    queryWords[i + 2] = "";
                }
                queryWords[i] = "'" + date + "'";
                i = i + 2;
            }
        }
        query = quickFilterQuery + " " + String.join(" ", queryWords);
        logger.info(query);
        return query;
    }

    /***
     *
     * @param str: MySQL query String to be parsed
     * @param viewFilter: ViewFilter to return with Filter[] of query String in it
     * @return ViewFilter which the input string is parsed and is in as a Filter[]
     */
    public static ViewFilter parseFilteringQuery(String str, ViewFilter viewFilter) {
        String[] conditions = str.split("AND ");
        Map<String, Filter> filters = new HashMap<>(conditions.length);
        for (String condition : conditions) {
            int state = 0;
            String tableName = "";
            String columnName = "";
            String value = "";
            String tempValue = "";
            String type = null;
            String path = null;
            ArrayList<String> selectedOptions = new ArrayList<>();
            Boolean range = false;
            Boolean empty = false;
            Boolean notEmpty = false;
            Boolean exact = false;
            Boolean longWord = false;
            Boolean f2 = false;
            NameValue filter2 = null;
            NameValue filter1 = null;
            if (StringUtils.isBlank(condition)) {
                continue;
            }
            String[] words = condition.split("(\\s+)");
            for (String word : words) {
                if (StringUtils.isNotBlank(word)) {
                    switch (state) {
                        case 0:
                            if (word.equals("(")) {// beginning of an options query
                                state = 2;
                                break;
                            }
                            else if (word.equals("JSON_EXTRACT")) {// beginning of an additional fields query
                                type = Filter.ADDITIONAL_VALUES;
                                state = 16;
                                break;
                            }
                            else if (word.equals("NOT")) {
                                state = 24;
                                break;
                            }
                            else {// beginning of other types of query
                                tableName = word.substring(0, word.indexOf("."));
                                columnName = word.substring(word.indexOf(".") + 1);
                                state = 1;
                            }
                            break;
                        case 1:
                            if (word.equals("=")) { // exact match selected in the frontend
                                exact = true;
                                range = false;
                                state = 3;
                                break;
                            }
                            else if (word.equals("<=") || word.equals(">=")) { // range selected in the frontend
                                exact = false;
                                range = true;
                                state = 4;
                                if (word.equals("<=")) {
                                    f2 = true;
                                }
                                break;
                            }
                            else if (word.toUpperCase().equals(Filter.IS)) { // empty or not empty selected in the frontend
                                exact = false;
                                range = false;
                                state = 5;
                                break;
                            }
                            else if (word.equals("LIKE")) { // either checkbox or exact match not selected in the frontend
                                exact = false;
                                range = false;
                                state = 8;
                                break;
                            }
                            else if (word.equals("<=>")) {
                                exact = false;
                                range = false;
                                type = Filter.CHECKBOX;
                                state = 8;
                                break;
                            }
                            break;

                        case 2:// type is OPTIONS
                            type = Filter.OPTIONS;
                            String[] names = word.split("\\.");
                            tableName = names[0];
                            columnName = names[1];
                            state = 12;
                            break;

                        case 3: // exact matching value
                            if (word.equals("today")) {
                                value = getDate();
                                state = 22;
                                break;
                            }
                            else {
                                tempValue = word;
                                if (!longWord) {
                                    if (tempValue.contains("'")) {
                                        if (tempValue.indexOf("'") != tempValue.lastIndexOf("'")) {
                                            value = trimValue(tempValue);
                                            state = 11;
                                            break;
                                        }
                                        else {
                                            longWord = true;
                                            value += trimValue(tempValue) + " ";
                                        }
                                    }
                                }
                                else if (longWord && tempValue.contains("'")) {
                                    value += trimValue(tempValue) + " ";
                                    longWord = false;
                                    value = value.substring(0, value.length() - 1);//remove the last added space
                                    state = 11;
                                    break;
                                }
                                else if (longWord) {
                                    value += tempValue + " ";
                                }
                            }
                            break;

                        case 4: // range value
                            if (word.equals("today")) {
                                state = 22;
                                type = Filter.DATE;
                                break;
                            }
                            value = trimValue(word);
                            if (isValidDate(value, false)) {
                                type = Filter.DATE;
                            }
                            state = 11;
                            break;

                        case 5://if query contains "IS NOT sth"
                            if (word.toUpperCase().equals(Filter.NOT)) {
                                empty = false;
                                state = 6;
                                break;

                            }//if query contains "IS NULL" then empty was selected
                            else if (word.toUpperCase().equals(Filter.NULL)) {
                                notEmpty = false;
                                empty = true;
                                state = 10;
                                break;
                            }
                            break;
                        case 6:// query contains "IS NOT NULL" so not empty was selected in the frontend
                            if (word.toUpperCase().equals(Filter.NULL)) {
                                notEmpty = true;
                                empty = false;
                                state = 7;
                                break;
                            }
                            break;
                        case 7:
                            break;

                        case 8:// query contained word "LIKE", exact match is false then
                            exact = false;
                            if (word.equals("'1'") || (word.equals("1") && (Filter.CHECKBOX.equals(type)))) {// check boxes are either 1 or 0
                                if (StringUtils.isNotBlank(type)) {
                                    filter2 = new NameValue(columnName, true);
                                }
                                else {
                                    filter1 = new NameValue(columnName, true);
                                }
                                type = Filter.CHECKBOX;
                                state = 9;
                                break;
                            }
                            else {// "LIKE %?% query
                                value = word;
                                if (value.contains("%")) {
                                    exact = false;
                                    int first = value.indexOf('%');
                                    int last = value.lastIndexOf('%');
                                    if (first != -1) {
                                        value = value.substring(first + 1, last);
                                    }
                                }

                                state = 11;
                            }
                            break;
                        case 9:// termination state
                            break;
                        case 10:// termination state
                            break;
                        case 11:// termination state
                            if (word.equals("+")) {
                                state = 23;
                                break;
                            }
                            else if (word.equals("-")) {
                                state = 26;
                                break;
                            }
                            break;
                        case 12:
                            state = 14;
                            break;
                        case 13:
                            break;
                        case 14:// finding the next selected option
                            int first = word.indexOf('\'');
                            int last = word.lastIndexOf('\'');
                            if (first != -1) {
                                word = word.substring(first + 1, last);
                            }
                            selectedOptions.add(word);
                            state = 15;
                            break;
                        case 15:
                            if (word.equals(Filter.OR)) {// look for another selected option
                                state = 2;
                                break;
                            }
                            else if (word.equals(")")) {// end of selected options
                                state = 13;
                                break;
                            }
                            break;
                        case 16: // beginning of additional fields query
                            if (word.equals("(")) {
                                state = 17;
                            }
                            break;
                        case 17:// in additional fields query find the table and the column
                            names = word.split("\\.");
                            tableName = names[0];
                            columnName = names[1];
                            state = 18;
                            break;
                        case 18:
                            if (word.equals(",")) {// need to look for the path in the query since it is a MySQL json query
                                state = 19;
                            }
                            break;
                        case 19:// finding the additional fields name
                            String[] paths = word.split("[$\\.']");
                            path = paths[3];
                            state = 20;
                            break;
                        case 20:// end of the json_extract query
                            if (word.equals(")")) {
                                state = 21;
                            }
                            break;
                        case 21:// finding the  exact match value
                            if (word.equals("=")) {
                                state = 3;
                                break;
                            }
                            else if (word.equals(Filter.LIKE)) {
                                state = 8;
                                break;
                            }
                            break;
                        case 22:
                            if (word.equals("+")) {
                                state = 23;
                                break;
                            }
                            else if (word.equals("-")){
                                state = 26;
                                break;
                            }
                            break;
                        case 23:
                            if (word.indexOf("d") != -1) {
                                String days = word;
                                days = days.replace("d", "");
                                int numberOfDays = Integer.parseInt(days);
                                value = getDate(numberOfDays, '+');
                                state = 25;
                                break;
                            }
                            break;
                        case 24:
                            names = word.split("\\.");
                            tableName = names[0];
                            columnName = names[1];
                            state = 1;
                            break;
                        case 25:
                            break;
                        case 26:
                            if (word.indexOf("d") != -1) {
                                String days = word;
                                days = days.replace("d", "");
                                int numberOfDays = Integer.parseInt(days);
                                value = getDate(numberOfDays, '-');
                                state = 25;
                                break;
                            }
                            break;
                    }
                }
            }
            if (state != 9 && state != 11 && state != 10 && state != 13 && state != 7 && state != 22 && state != 25) {// terminal states
                throw new RuntimeException("Query parsing ended in bad state: " + state);
            }
            String columnKey = columnName;
            if (StringUtils.isNotBlank(tableName)) {
                columnKey = tableName.concat(DBConstants.ALIAS_DELIMITER).concat(columnName);
            }
            columnName = PatchUtil.getDataBaseMap().containsKey(columnKey) ? PatchUtil.getDataBaseMap().get(columnKey) : columnName;
            if (filters.containsKey(columnName) && (type == null || !type.equals(Filter.ADDITIONAL_VALUES))) {
                Filter filter = filters.get(columnName);
                filter.range = true;
                if (StringUtils.isNotBlank(value)) {
                    filter.filter2 = new NameValue(columnName, value);
                }
                filter.notEmpty = notEmpty;
                filter.empty = empty;
                filters.put(columnName, filter);
            }
            else {
                Filter filter = new Filter();
                filter.exactMatch = exact;
                filter.range = range;
                filter.notEmpty = notEmpty;
                filter.empty = empty;
                filter.participantColumn = new ParticipantColumn(columnName, tableName);
                String[] b = new String[selectedOptions.size()];
                filter.selectedOptions = selectedOptions.toArray(b);
                filter.type = type == null ? Filter.TEXT : type;
                if (isValidDate(value, false)) {
                    type = Filter.DATE;
                }
                if (Filter.ADDITIONAL_VALUES.equals(type)) {
                    filter.participantColumn = new ParticipantColumn(path, tableName);
                }
                if (!Filter.CHECKBOX.equals(type)) {
                    if (!f2) {
                        filter.filter1 = new NameValue(columnName, value);
                    }
                    if (path != null && !f2) {
                        filter.filter2 = new NameValue(path, "");
                    }
                    if (f2) {
                        filter.filter1 = new NameValue(columnName, null);
                        filter.filter2 = new NameValue(columnName, value);
                    }
                }
                else {
                    if (filter1 != null && !f2) {
                        filter.filter1 = filter1;
                    }
                    else if (filter2 != null || f2) {
                        filter.filter2 = filter2;
                    }
                }
                filters.put(columnName, filter);
            }
        }
        String newQuery = parseToFrontEndQuery(str);
        Filter[] a = new Filter[filters.size()];
        ViewFilter result = new ViewFilter(viewFilter.filterName, viewFilter.columns, viewFilter.id, viewFilter.fDeleted, viewFilter.shared,
                viewFilter.userId, filters.values().toArray(a), viewFilter.parent,
                viewFilter.icon, viewFilter.quickFilterName, newQuery, null, viewFilter.realmId);
        return result;
    }


    private static String parseToFrontEndQuery(String str) {

        if (StringUtils.isNotBlank(str)) {
            String[] words = str.split("[ ]");
            Set<String> map = PatchUtil.getTableAliasArray();
            //        boolean ES = true;
            for (int i = 0; i < words.length - 1; i++) {
                if ((words[i].contains("."))) {
                    //                ES = false;
                    String[] w = words[i].split("[\\.]");
                    if (map.contains(w[0])) {
                        String s = w[0] + "." + PatchUtil.getDataBaseMap().get(w[0] + "." + w[1]);
                        words[i] = s;
                        continue;
                    }
                }
            }
            return String.join(" ", words);
        }
        return str;
    }

    public static boolean isValidDate(String inDate, boolean frontend) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        if (frontend) {
            dateFormat = new SimpleDateFormat("MM/dd/yyyy");
        }
        dateFormat.setLenient(false);
        try {
            dateFormat.parse(inDate.trim());
        }
        catch (ParseException pe) {
            return false;
        }
        return true;
    }


    public static String changeFieldsInQuery(String filterQuery) {
        String[] words = filterQuery.split("\\s+");
        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            if (PatchUtil.getColumnNameMap().containsKey(word)) {
                words[i] = PatchUtil.getColumnNameMap().get(word).tableAlias + DBConstants.ALIAS_DELIMITER + PatchUtil.getColumnNameMap().get(word).columnName;
            }
            else if (word.contains("'") && word.charAt(0) == '\'' && word.charAt(word.length() - 1) == '\'') {
                String temp = word.substring(1, word.length() - 1);
                if (isValidDate(temp, true)) {
                    try {
                        words[i] = "'" + SystemUtil.changeDateFormat("MM/dd/yyyy", "yyyy-MM-dd", temp) + "'";
                    }
                    catch (Exception pe) {
                        throw new RuntimeException(pe);
                    }
                }

            }
        }
        return String.join(" ", words);
    }

    public static String getDefaultFilterForUser(@NonNull String userId, @NonNull String parent) {
        SimpleResult results = inTransaction((conn) -> {
            SimpleResult dbVals = new SimpleResult();
            String query = SQL_GET_DEFAULT_FILTER.replace("#", userId);
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, parent);
                //                stmt.setString(2, parent);
                try {
                    ResultSet rs = stmt.executeQuery();
                    if (rs.next()) {
                        dbVals.resultValue = rs.getString(DBConstants.DISPLAY_NAME_FILTER);
                    }
                }
                catch (Exception e) {
                    dbVals.resultException = e;
                }
            }
            catch (SQLException ex) {
                dbVals.resultException = ex;
            }
            return dbVals;
        });
        if (results.resultException != null) {
            throw new RuntimeException(results.resultException);
        }
        return (String) results.resultValue;
    }

    public static Object setDefaultFilter(String filterName, String userMail, String parent) {
        String defaultUsers = null;
        if (StringUtils.isNotBlank(filterName))
        //get all default users of the new default filter
        {
            defaultUsers = getDefaultUsers(filterName, parent);
        }

        //get previous default filter
        String oldDefaultFilter = getDefaultFilterForUser(userMail, parent);

        //update new default filter
        String newUsers = userMail;
        if (StringUtils.isNotBlank(defaultUsers)) {
            newUsers = defaultUsers + "," + userMail;
        }
        if (StringUtils.isNotBlank(filterName)) {
            updateDefaultUsers(filterName, parent, newUsers);
        }

        //update old default filter
        if (StringUtils.isNotBlank(oldDefaultFilter)) {
            String defaultUsersOfOldFilter = getDefaultUsers(oldDefaultFilter, parent);
            String newDefaultUsersOfOldFilter = null;
            if (!defaultUsersOfOldFilter.equals(userMail)) {
                //more than that user had this filter as default
                if (defaultUsersOfOldFilter.endsWith(userMail)) {
                    newDefaultUsersOfOldFilter = defaultUsersOfOldFilter.replace("," + userMail, "");
                }
                else {
                    newDefaultUsersOfOldFilter = defaultUsersOfOldFilter.replace(userMail + ",", "");
                }
            }
            try {
                updateDefaultUsers(oldDefaultFilter, parent, newDefaultUsersOfOldFilter);
            }
            catch (RuntimeException e) {
                //couldn't remove user from old default filter - remove user from new default filter
                updateDefaultUsers(filterName, parent, defaultUsers);
            }
        }
        return new Result(200);
    }

    private static String updateDefaultUsers(@NonNull String filterName, @NonNull String parent, String defaultUsers) {
        SimpleResult results = inTransaction((conn) -> {
            SimpleResult dbVals = new SimpleResult();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_UPDATE_DEFAULT_FILTER)) {
                stmt.setString(1, defaultUsers);
                stmt.setString(2, filterName);
                stmt.setString(3, parent);
                int rows = stmt.executeUpdate();
                if (rows != 1) {
                    throw new RuntimeException("Updated " + rows + " rows");
                }
            }
            catch (SQLException ex) {
                dbVals.resultException = ex;
            }
            return dbVals;
        });

        if (results.resultException != null) {
            throw new RuntimeException("Error adding new view", results.resultException);
        }
        return (String) results.resultValue;
    }

    private static String getDefaultUsers(@NonNull String filterName, @NonNull String parent) {
        SimpleResult results = inTransaction((conn) -> {
            SimpleResult dbVals = new SimpleResult();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_SELECT_DEFAULT_FILTER_USERS)) {
                stmt.setString(1, filterName);
                stmt.setString(2, parent);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        dbVals.resultValue = rs.getString(DBConstants.DEFAULT_USERS);
                    }
                }
            }
            catch (SQLException ex) {
                dbVals.resultException = ex;
            }
            return dbVals;
        });

        if (results.resultException != null) {
            throw new RuntimeException("Error adding new view", results.resultException);
        }
        return (String) results.resultValue;
    }

    private static String trimValue(String value) {
        if (value.contains("'")) {
            value = trimFromCharSequence(value, '\'');
        }
        if (value.contains("%")) {
            value = trimFromCharSequence(value, '%');
        }
        return value;
    }

    private static String trimFromCharSequence(String value, char charSeq) {
        int first = value.indexOf(charSeq);
        int last = value.lastIndexOf(charSeq);
        if (first != -1) {
            if (last != first) {
                value = value.substring(first + 1, last);
            }
            else {
                if (first == 0) {
                    value = value.substring(first + 1);
                }
                else {
                    value = value.substring(0, last);
                }
            }
        }
        return value;
    }

    public static String getDate() {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDateTime now = LocalDateTime.now();
        return (dtf.format(now));
    }

    public static String getDate(LocalDateTime date) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        return (dtf.format(date));
    }

    public static String getDate(int numberOfDays, char operator) {
        if (operator == '-') {
            numberOfDays *= -1;
        }
        LocalDateTime today = LocalDateTime.now();     //Today
        LocalDateTime tomorrow = today.plusDays(numberOfDays);
        return getDate(tomorrow);
    }
}

