package org.broadinstitute.dsm.model.filter.participant;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import com.google.gson.Gson;
import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.broadinstitute.dsm.db.DDPInstance;
import org.broadinstitute.dsm.db.ViewFilter;
import org.broadinstitute.dsm.db.dao.ddp.participant.ParticipantDataDao;
import org.broadinstitute.dsm.db.dto.ddp.participant.ParticipantDataDto;
import org.broadinstitute.dsm.db.structure.DBElement;
import org.broadinstitute.dsm.model.Filter;
import org.broadinstitute.dsm.model.ParticipantWrapper;
import org.broadinstitute.dsm.model.filter.BaseFilter;
import org.broadinstitute.dsm.model.filter.Filterable;
import org.broadinstitute.dsm.statics.DBConstants;
import org.broadinstitute.dsm.statics.RequestParameter;
import org.broadinstitute.dsm.statics.RoutePath;
import org.broadinstitute.dsm.util.ElasticSearchUtil;
import org.broadinstitute.dsm.util.ParticipantUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.QueryParamsMap;

public abstract class BaseFilterParticipantList extends BaseFilter implements Filterable<ParticipantWrapper> {

    private static final Logger logger = LoggerFactory.getLogger(BaseFilterParticipantList.class);
    public static final String PARTICIPANT_DATA = "participantData";
    public static final String OPTIONS = "OPTIONS";
    public static final String RADIO = "RADIO";
    public static final String PARTICIPANTS = "PARTICIPANTS";
    protected static final Gson GSON = new Gson();
    private static final Pattern DATE_PATTERN = Pattern.compile(
            "^\\d{4}-\\d{2}-\\d{2}$");
    private final ParticipantDataDao participantDataDao;

    {
        participantDataDao = new ParticipantDataDao();
    }

    public BaseFilterParticipantList() {
        super(null);
    }

    public BaseFilterParticipantList(String jsonBody) {
        super(jsonBody);
    }


    protected List<ParticipantWrapper> filterParticipantList(Filter[] filters, Map<String, DBElement> columnNameMap, @NonNull DDPInstance instance) {
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

    public void addParticipantDataFilters(Map<String, String> queryConditions,
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
            Map<String, String> dataMap = GSON.fromJson(data, Map.class);
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

    private StringBuilder getNewCondition(Map<String, String> participantIdsForQuery) {
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

    private void addConditionForRange(Filter filter, String fieldName, Map<String, String> dataMap, Map<String, String> participantIdsForQuery, String ddpParticipantId) {
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

    private boolean isInDateRange(String fieldName, Map<String, String> dataMap, Object rangeValue1, Object rangeValue2) {
        return (dataMap.get(fieldName) != null)
                && rangeValue1 instanceof String && rangeValue2 instanceof String
                && DATE_PATTERN.matcher((String) rangeValue1).matches() && DATE_PATTERN.matcher((String) rangeValue2).matches()
                && LocalDate.parse(dataMap.get(fieldName)).compareTo(LocalDate.parse((String) rangeValue1)) >= 0
                && ((LocalDate.parse(dataMap.get(fieldName)).compareTo(LocalDate.parse((String) rangeValue1)) >= 0
                && LocalDate.parse(dataMap.get(fieldName)).compareTo(LocalDate.parse((String) rangeValue2)) < 0)
                || (LocalDate.parse(dataMap.get(fieldName)).compareTo(LocalDate.parse((String) rangeValue2)) >= 0
                && LocalDate.parse(dataMap.get(fieldName)).compareTo(LocalDate.parse((String) rangeValue1)) < 0));
    }

    private boolean isInNumberRange(String fieldName, Map<String, String> dataMap, Object rangeValue1, Object rangeValue2) {
        boolean dataIsNumber = dataMap.get(fieldName) != null && NumberUtils.isNumber(dataMap.get(fieldName));
        boolean moreThanFirstNumber = dataIsNumber && rangeValue1 instanceof Double && Double.compare(Double.parseDouble(dataMap.get(fieldName)), (Double) rangeValue1) >= 0;
        boolean moreThanSecondNumber = dataIsNumber && rangeValue2 instanceof Double && Double.compare(Double.parseDouble(dataMap.get(fieldName)), (Double) rangeValue2) >= 0;
        //range will be starting from the lower number up until the higher number
        return (moreThanFirstNumber && !moreThanSecondNumber) || (moreThanSecondNumber && !moreThanFirstNumber);
    }

}
