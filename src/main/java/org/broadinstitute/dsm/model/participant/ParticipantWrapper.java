package org.broadinstitute.dsm.model.participant;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import lombok.Data;
import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.dsm.db.*;
import org.broadinstitute.dsm.db.dao.ddp.instance.DDPInstanceDao;
import org.broadinstitute.dsm.db.dao.ddp.participant.ParticipantDataDao;
import org.broadinstitute.dsm.db.dto.ddp.instance.DDPInstanceDto;
import org.broadinstitute.dsm.db.dto.ddp.participant.ParticipantDataDto;
import org.broadinstitute.dsm.model.Filter;
import org.broadinstitute.dsm.model.elasticsearch.ESProfile;
import org.broadinstitute.dsm.model.elasticsearch.ElasticSearch;
import org.broadinstitute.dsm.model.elasticsearch.ElasticSearchable;
import org.broadinstitute.dsm.model.participant.data.FamilyMemberConstants;
import org.broadinstitute.dsm.model.at.DefaultValues;
import org.broadinstitute.dsm.statics.DBConstants;
import org.broadinstitute.dsm.util.ElasticSearchUtil;
import org.broadinstitute.dsm.util.ParticipantUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Data
public class ParticipantWrapper {

    private static final Logger logger = LoggerFactory.getLogger(ParticipantWrapper.class);

    public static final String BY_DDP_PARTICIPANT_ID_IN = " AND request.ddp_participant_id IN (\"";
    public static final String ORDER_AND_LIMIT = " ORDER BY request.dsm_kit_request_id desc LIMIT 5000";
    public static final int DEFAULT_ROWS_ON_PAGE = 50;

    private ParticipantWrapperPayload participantWrapperPayload;
    private ElasticSearchable elasticSearchable;

    private List<ElasticSearch> participantsEsDataWithinRange = new ArrayList<>();
    private List<Participant> participants = new ArrayList<>();
    private Map<String, List<MedicalRecord>> medicalRecords = new HashMap<>();
    private Map<String, List<OncHistoryDetail>> oncHistoryDetails = new HashMap<>();
    private Map<String, List<KitRequestShipping>> kitRequests = new HashMap<>();
    private Map<String, List<AbstractionActivity>> abstractionActivities = new HashMap<>();
    private Map<String, List<AbstractionGroup>> abstractionSummary = new HashMap<>();
    private Map<String, List<ElasticSearch>> proxiesByParticipantIds = new HashMap<>();
    private Map<String, List<ParticipantDataDto>> participantDataByParticipantIds = new HashMap<>();

    public ParticipantWrapper(ParticipantWrapperPayload participantWrapperPayload, ElasticSearchable elasticSearchable) {
        this.participantWrapperPayload = Objects.requireNonNull(participantWrapperPayload);
        this.elasticSearchable = Objects.requireNonNull(elasticSearchable);
    }
    
    //useful to get participant from ES if short id is either hruid or legacy short id
    public static Optional<ParticipantWrapperDto> getParticipantByShortId(DDPInstance ddpInstance, String participantShortId) {
        Optional<ParticipantWrapperDto> maybeParticipant;

        if (ParticipantUtil.isHruid(participantShortId)) {
            maybeParticipant = getParticipantFromESByHruid(ddpInstance, participantShortId);
        } else {
            maybeParticipant = getParticipantFromESByLegacyShortId(ddpInstance, participantShortId);
        }
        return maybeParticipant;
    }

//    public static List<ParticipantWrapperDto> getFilteredList(@NonNull DDPInstance instance, Map<String, String> filters) {
//        logger.info("Getting list of participant information");
//
//        if (StringUtils.isBlank(instance.getParticipantIndexES())) {
//            throw new RuntimeException("No participant index setup in ddp_instance table for " + instance.getName());
//        }
//        DefaultValues defaultValues;
//        if (filters == null) {
//            Map<String, Map<String, Object>> participantESData = getESData(instance);
//            Map<String, Participant> participants = Participant.getParticipants(instance.getName());
//            Map<String, List<MedicalRecord>> medicalRecords = null;
//            Map<String, List<OncHistoryDetail>> oncHistoryDetails = null;
//            Map<String, List<KitRequestShipping>> kitRequests = null;
//
//            if (instance.isHasRole()) { //only needed if study has mr&tissue tracking
//                medicalRecords = MedicalRecord.getMedicalRecords(instance.getName());
//                oncHistoryDetails = OncHistoryDetail.getOncHistoryDetails(instance.getName());
//            }
//            if (DDPInstanceDao.getRole(instance.getName(), DBConstants.KIT_REQUEST_ACTIVATED)) { //only needed if study is shipping samples per DSM
//                kitRequests = KitRequestShipping.getKitRequests(instance, ORDER_AND_LIMIT);
//            }
//            Map<String, List<AbstractionActivity>> abstractionActivities = AbstractionActivity.getAllAbstractionActivityByRealm(instance.getName());
//            Map<String, List<AbstractionGroup>> abstractionSummary = AbstractionFinal.getAbstractionFinal(instance.getName());
//            Map<String, Map<String, Object>> proxyData = getProxyData(instance);
//            Map<String, List<ParticipantData>> participantData = ParticipantData.getParticipantData(instance.getName());
//
//            //if study is AT
//            if ("atcp".equals(instance.getName())) {
//                defaultValues = new DefaultValues(participantData, participantESData, instance, null);
//                participantData = defaultValues.addDefaultValues();
//            }
//
//            sortBySelfElseById(participantData);
//
//            List<String> baseList = new ArrayList<>(participantESData.keySet());
//
//            return ParticipantWrapperDto.addAllData(baseList, participantESData, participants, medicalRecords, oncHistoryDetails, kitRequests, abstractionActivities, abstractionSummary, proxyData, participantData);
//        }
//        else {
//            //no filters, return all participants which came from ES with DSM data added to it
//            Map<String, Map<String, Object>> participantESData = null;
//            Map<String, Participant> participants = null;
//            Map<String, List<MedicalRecord>> medicalRecords = null;
//            Map<String, List<OncHistoryDetail>> oncHistories = null;
//            Map<String, List<KitRequestShipping>> kitRequests = null;
//            Map<String, List<AbstractionActivity>> abstractionActivities = null;
//            Map<String, List<AbstractionGroup>> abstractionSummary = null;
//            Map<String, Map<String, Object>> proxyData = null;
//            Map<String, List<ParticipantData>> participantData = null;
//            List<String> baseList = null;
//            //filter the lists depending on filter
//            for (String source : filters.keySet()) {
//                if (StringUtils.isNotBlank(filters.get(source))) {
//                    if (DBConstants.DDP_PARTICIPANT_ALIAS.equals(source)) {
//                        participants = Participant.getParticipants(instance.getName(), filters.get(source));
//                        baseList = getCommonEntries(baseList, new ArrayList<>(participants.keySet()));
//                    }
//                    else if (DBConstants.DDP_MEDICAL_RECORD_ALIAS.equals(source)) {
//                        medicalRecords = MedicalRecord.getMedicalRecords(instance.getName(), filters.get(source));
//                        baseList = getCommonEntries(baseList, new ArrayList<>(medicalRecords.keySet()));
//                    }
//                    else if (DBConstants.DDP_ONC_HISTORY_DETAIL_ALIAS.equals(source)) {
//                        oncHistories = OncHistoryDetail.getOncHistoryDetails(instance.getName(), filters.get(source));
//                        baseList = getCommonEntries(baseList, new ArrayList<>(oncHistories.keySet()));
//                    }
//                    else if (DBConstants.DDP_KIT_REQUEST_ALIAS.equals(source)) {
//                        kitRequests = KitRequestShipping.getKitRequests(instance, filters.get(source));
//                        baseList = getCommonEntries(baseList, new ArrayList<>(kitRequests.keySet()));
//                    }
//                    else if (DBConstants.DDP_PARTICIPANT_DATA_ALIAS.equals(source)) {
//                        participantData = ParticipantData.getParticipantData(instance.getName(), filters.get(source));
//                        baseList = getCommonEntries(baseList, new ArrayList<>(participantData.keySet()));
//
//                        //if study is AT
//                        if ("atcp".equals(instance.getName())) {
//                            defaultValues =
//                                    new DefaultValues(participantData, participantESData, instance, filters.get(source));
//                            participantData = defaultValues.addDefaultValues();
//                        }
//                    }
//                    else if (DBConstants.DDP_ABSTRACTION_ALIAS.equals(source)) {
//                        abstractionActivities = AbstractionActivity.getAllAbstractionActivityByRealm(instance.getName(), filters.get(source));
//                        baseList = getCommonEntries(baseList, new ArrayList<>(abstractionActivities.keySet()));
//                    }
//                    //                else if (DBConstants.DDP_ABSTRACTION_ALIAS.equals(source)) {
//                    //                    abstractionSummary = AbstractionFinal.getAbstractionFinal(instance.getName(), filters.get(source));
//                    //                    baseList = getCommonEntries(baseList, new ArrayList<>(abstractionSummary.keySet()));
//                    //                }
//                    else { //source is not of any study-manager table so it must be ES
//                        participantESData = getParticipantESDataConsideringNumberOfParameters(instance, filters, source);
//                        baseList = getCommonEntries(baseList, new ArrayList<>(participantESData.keySet()));
//                    }
//                }
//            }
//            //get all the list which were not filtered
//            if (participantESData == null) {
//                participantESData = new HashMap<>();
//                //get only pts for the filtered data
//                if (baseList != null && !baseList.isEmpty()) {
//                    //ES can only filter for 1024 (too_many_clauses: maxClauseCount is set to 1024)
//                    if (baseList.size() > Filter.THOUSAND) {
//                        //make sub-searches
//                        Collection<List<String>> partitionBaseList = partitionBasedOnSize(baseList, Filter.THOUSAND);
//                        for (Iterator i = partitionBaseList.iterator(); i.hasNext();) {
//                            List<String> baseListPart = ((List<String>) i.next());
//                            participantESData = addParticipantESData(instance, baseListPart, participantESData, ElasticSearchUtil.BY_GUID, ElasticSearchUtil.BY_GUIDS);
//                            if (instance.isMigratedDDP()) {//also check for legacyAltPid
//                                participantESData = addParticipantESData(instance, baseListPart, participantESData, ElasticSearchUtil.BY_LEGACY_ALTPID, ElasticSearchUtil.BY_LEGACY_ALTPIDS);
//                            }
//                        }
//                    }
//                    else {
//                        //just search
//                        participantESData = addParticipantESData(instance, baseList, participantESData, ElasticSearchUtil.BY_GUID, ElasticSearchUtil.BY_GUIDS);
//                        if (instance.isMigratedDDP()) {//also check for legacyAltPid
//                            participantESData = addParticipantESData(instance, baseList, participantESData, ElasticSearchUtil.BY_LEGACY_ALTPID, ElasticSearchUtil.BY_LEGACY_ALTPIDS);
//                        }
//                    }
//                }
//                else {
//                    //get all pts
//                    participantESData = getESData(instance);
//                }
//            }
//            if (participants == null) {
//                participants = Participant.getParticipants(instance.getName());
//            }
//            if (medicalRecords == null && instance.isHasRole()) {
//                medicalRecords = MedicalRecord.getMedicalRecords(instance.getName());
//            }
//            if (oncHistories == null && instance.isHasRole()) {
//                oncHistories = OncHistoryDetail.getOncHistoryDetails(instance.getName());
//            }
//            if (kitRequests == null && DDPInstanceDao.getRole(instance.getName(), DBConstants.KIT_REQUEST_ACTIVATED)) { //only needed if study is shipping samples per DSM
//                //get only kitRequests for the filtered pts
//                if (participantESData != null && !participantESData.isEmpty()) {
//                    String filter = Arrays.stream(participantESData.keySet().toArray(new String[0])).collect(Collectors.joining("\",\""));
//                    logger.info("About to query for kits from " + participantESData.size() + " participants");
//                    kitRequests = KitRequestShipping.getKitRequests(instance, BY_DDP_PARTICIPANT_ID_IN + filter + "\")");
//                }
//                else {
//                    //get all kitRequests
//                    kitRequests = KitRequestShipping.getKitRequests(instance, ORDER_AND_LIMIT);
//                }
//            }
//            if (participantData == null) {
//                participantData = ParticipantData.getParticipantData(instance.getName());
//
//                //if study is AT
//                if ("atcp".equals(instance.getName())) {
//                    defaultValues = new DefaultValues(participantData, participantESData, instance, null);
//                    participantData = defaultValues.addDefaultValues();
//                }
//            }
//            if (abstractionActivities == null) {
//                abstractionActivities = AbstractionActivity.getAllAbstractionActivityByRealm(instance.getName());
//            }
//            if (abstractionSummary == null) {
//                abstractionSummary = AbstractionFinal.getAbstractionFinal(instance.getName());
//            }
//            if (proxyData == null) {
//                proxyData = getProxyData(instance);
//            }
//
//            baseList = getCommonEntries(baseList, new ArrayList<>(participantESData.keySet()));
//
//            sortBySelfElseById(participantData);
//
//            //bring together all the information
//            return ParticipantWrapperDto.addAllData(baseList, participantESData, participants, medicalRecords, oncHistories, kitRequests, abstractionActivities, abstractionSummary, proxyData, participantData);
//        }
//    }

    public List<ParticipantWrapperDto> getFilteredList() {
        logger.info("Getting list of participant information");

        DDPInstanceDto ddpInstanceDto = participantWrapperPayload.getDdpInstanceDto()
                .orElseThrow();

        if (StringUtils.isBlank(ddpInstanceDto.getEsParticipantIndex())) {
            throw new RuntimeException("No participant index setup in ddp_instance table for " + ddpInstanceDto.getInstanceName());
        }

        DDPInstance ddpInstance = DDPInstance.getDDPInstance(ddpInstanceDto.getInstanceName());

        return participantWrapperPayload.getFilter()
                .map(filter -> (List<ParticipantWrapperDto>) new ArrayList<ParticipantWrapperDto>())
                .orElseGet(() -> {
                    fetchAndPrepareData(ddpInstanceDto, ddpInstance);
                    //if study is AT TODO
//                    if ("atcp".equals(ddpInstance.getName())) {
//                        defaultValues = new DefaultValues(participantData, participantESData, instance, null);
//                        participantData = defaultValues.addDefaultValues();
//                    }
                    sortBySelfElseById(participantDataByParticipantIds.values());
                    return collectData();
                });

//        DefaultValues defaultValues;
//        if (filters == null) {
//            Map<String, Map<String, Object>> participantESData = getESData(instance);
//            Map<String, Participant> participants = Participant.getParticipants(instance.getName());
//            Map<String, List<MedicalRecord>> medicalRecords = null;
//            Map<String, List<OncHistoryDetail>> oncHistoryDetails = null;
//            Map<String, List<KitRequestShipping>> kitRequests = null;
//
//            if (instance.isHasRole()) { //only needed if study has mr&tissue tracking
//                medicalRecords = MedicalRecord.getMedicalRecords(instance.getName());
//                oncHistoryDetails = OncHistoryDetail.getOncHistoryDetails(instance.getName());
//            }
//            if (DDPInstanceDao.getRole(instance.getName(), DBConstants.KIT_REQUEST_ACTIVATED)) { //only needed if study is shipping samples per DSM
//                kitRequests = KitRequestShipping.getKitRequests(instance, ORDER_AND_LIMIT);
//            }
//            Map<String, List<AbstractionActivity>> abstractionActivities = AbstractionActivity.getAllAbstractionActivityByRealm(instance.getName());
//            Map<String, List<AbstractionGroup>> abstractionSummary = AbstractionFinal.getAbstractionFinal(instance.getName());
//            Map<String, Map<String, Object>> proxyData = getProxyData(instance);
//            Map<String, List<ParticipantData>> participantData = ParticipantData.getParticipantData(instance.getName());
//
//            //if study is AT
//            if ("atcp".equals(instance.getName())) {
//                defaultValues = new DefaultValues(participantData, participantESData, instance, null);
//                participantData = defaultValues.addDefaultValues();
//            }
//
//            sortBySelfElseById(participantData);
//
//            List<String> baseList = new ArrayList<>(participantESData.keySet());
//
//            return ParticipantWrapperDto.addAllData(baseList, participantESData, participants, medicalRecords, oncHistoryDetails, kitRequests, abstractionActivities, abstractionSummary, proxyData, participantData);
//        }
//        else {
//            //no filters, return all participants which came from ES with DSM data added to it
//            Map<String, Map<String, Object>> participantESData = null;
//            Map<String, Participant> participants = null;
//            Map<String, List<MedicalRecord>> medicalRecords = null;
//            Map<String, List<OncHistoryDetail>> oncHistories = null;
//            Map<String, List<KitRequestShipping>> kitRequests = null;
//            Map<String, List<AbstractionActivity>> abstractionActivities = null;
//            Map<String, List<AbstractionGroup>> abstractionSummary = null;
//            Map<String, Map<String, Object>> proxyData = null;
//            Map<String, List<ParticipantData>> participantData = null;
//            List<String> baseList = null;
//            //filter the lists depending on filter
//            for (String source : filters.keySet()) {
//                if (StringUtils.isNotBlank(filters.get(source))) {
//                    if (DBConstants.DDP_PARTICIPANT_ALIAS.equals(source)) {
//                        participants = Participant.getParticipants(instance.getName(), filters.get(source));
//                        baseList = getCommonEntries(baseList, new ArrayList<>(participants.keySet()));
//                    }
//                    else if (DBConstants.DDP_MEDICAL_RECORD_ALIAS.equals(source)) {
//                        medicalRecords = MedicalRecord.getMedicalRecords(instance.getName(), filters.get(source));
//                        baseList = getCommonEntries(baseList, new ArrayList<>(medicalRecords.keySet()));
//                    }
//                    else if (DBConstants.DDP_ONC_HISTORY_DETAIL_ALIAS.equals(source)) {
//                        oncHistories = OncHistoryDetail.getOncHistoryDetails(instance.getName(), filters.get(source));
//                        baseList = getCommonEntries(baseList, new ArrayList<>(oncHistories.keySet()));
//                    }
//                    else if (DBConstants.DDP_KIT_REQUEST_ALIAS.equals(source)) {
//                        kitRequests = KitRequestShipping.getKitRequests(instance, filters.get(source));
//                        baseList = getCommonEntries(baseList, new ArrayList<>(kitRequests.keySet()));
//                    }
//                    else if (DBConstants.DDP_PARTICIPANT_DATA_ALIAS.equals(source)) {
//                        participantData = ParticipantData.getParticipantData(instance.getName(), filters.get(source));
//                        baseList = getCommonEntries(baseList, new ArrayList<>(participantData.keySet()));
//
//                        //if study is AT
//                        if ("atcp".equals(instance.getName())) {
//                            defaultValues =
//                                    new DefaultValues(participantData, participantESData, instance, filters.get(source));
//                            participantData = defaultValues.addDefaultValues();
//                        }
//                    }
//                    else if (DBConstants.DDP_ABSTRACTION_ALIAS.equals(source)) {
//                        abstractionActivities = AbstractionActivity.getAllAbstractionActivityByRealm(instance.getName(), filters.get(source));
//                        baseList = getCommonEntries(baseList, new ArrayList<>(abstractionActivities.keySet()));
//                    }
//                    //                else if (DBConstants.DDP_ABSTRACTION_ALIAS.equals(source)) {
//                    //                    abstractionSummary = AbstractionFinal.getAbstractionFinal(instance.getName(), filters.get(source));
//                    //                    baseList = getCommonEntries(baseList, new ArrayList<>(abstractionSummary.keySet()));
//                    //                }
//                    else { //source is not of any study-manager table so it must be ES
//                        participantESData = getParticipantESDataConsideringNumberOfParameters(instance, filters, source);
//                        baseList = getCommonEntries(baseList, new ArrayList<>(participantESData.keySet()));
//                    }
//                }
//            }
//            //get all the list which were not filtered
//            if (participantESData == null) {
//                participantESData = new HashMap<>();
//                //get only pts for the filtered data
//                if (baseList != null && !baseList.isEmpty()) {
//                    //ES can only filter for 1024 (too_many_clauses: maxClauseCount is set to 1024)
//                    if (baseList.size() > Filter.THOUSAND) {
//                        //make sub-searches
//                        Collection<List<String>> partitionBaseList = partitionBasedOnSize(baseList, Filter.THOUSAND);
//                        for (Iterator i = partitionBaseList.iterator(); i.hasNext();) {
//                            List<String> baseListPart = ((List<String>) i.next());
//                            participantESData = addParticipantESData(instance, baseListPart, participantESData, ElasticSearchUtil.BY_GUID, ElasticSearchUtil.BY_GUIDS);
//                            if (instance.isMigratedDDP()) {//also check for legacyAltPid
//                                participantESData = addParticipantESData(instance, baseListPart, participantESData, ElasticSearchUtil.BY_LEGACY_ALTPID, ElasticSearchUtil.BY_LEGACY_ALTPIDS);
//                            }
//                        }
//                    }
//                    else {
//                        //just search
//                        participantESData = addParticipantESData(instance, baseList, participantESData, ElasticSearchUtil.BY_GUID, ElasticSearchUtil.BY_GUIDS);
//                        if (instance.isMigratedDDP()) {//also check for legacyAltPid
//                            participantESData = addParticipantESData(instance, baseList, participantESData, ElasticSearchUtil.BY_LEGACY_ALTPID, ElasticSearchUtil.BY_LEGACY_ALTPIDS);
//                        }
//                    }
//                }
//                else {
//                    //get all pts
//                    participantESData = getESData(instance);
//                }
//            }
//            if (participants == null) {
//                participants = Participant.getParticipants(instance.getName());
//            }
//            if (medicalRecords == null && instance.isHasRole()) {
//                medicalRecords = MedicalRecord.getMedicalRecords(instance.getName());
//            }
//            if (oncHistories == null && instance.isHasRole()) {
//                oncHistories = OncHistoryDetail.getOncHistoryDetails(instance.getName());
//            }
//            if (kitRequests == null && DDPInstanceDao.getRole(instance.getName(), DBConstants.KIT_REQUEST_ACTIVATED)) { //only needed if study is shipping samples per DSM
//                //get only kitRequests for the filtered pts
//                if (participantESData != null && !participantESData.isEmpty()) {
//                    String filter = Arrays.stream(participantESData.keySet().toArray(new String[0])).collect(Collectors.joining("\",\""));
//                    logger.info("About to query for kits from " + participantESData.size() + " participants");
//                    kitRequests = KitRequestShipping.getKitRequests(instance, BY_DDP_PARTICIPANT_ID_IN + filter + "\")");
//                }
//                else {
//                    //get all kitRequests
//                    kitRequests = KitRequestShipping.getKitRequests(instance, ORDER_AND_LIMIT);
//                }
//            }
//            if (participantData == null) {
//                participantData = ParticipantData.getParticipantData(instance.getName());
//
//                //if study is AT
//                if ("atcp".equals(instance.getName())) {
//                    defaultValues = new DefaultValues(participantData, participantESData, instance, null);
//                    participantData = defaultValues.addDefaultValues();
//                }
//            }
//            if (abstractionActivities == null) {
//                abstractionActivities = AbstractionActivity.getAllAbstractionActivityByRealm(instance.getName());
//            }
//            if (abstractionSummary == null) {
//                abstractionSummary = AbstractionFinal.getAbstractionFinal(instance.getName());
//            }
//            if (proxyData == null) {
//                proxyData = getProxyData(instance);
//            }
//
//            baseList = getCommonEntries(baseList, new ArrayList<>(participantESData.keySet()));
//
//            sortBySelfElseById(participantData);
//
//            //bring together all the information
//            return ParticipantWrapperDto.addAllData(baseList, participantESData, participants, medicalRecords, oncHistories, kitRequests, abstractionActivities, abstractionSummary, proxyData, participantData);
//        }
    }

    private void fetchAndPrepareData(DDPInstanceDto ddpInstanceDto, DDPInstance ddpInstance) {
        participantsEsDataWithinRange = elasticSearchable.getParticipantsWithinRange(
                ddpInstanceDto.getEsParticipantIndex(),
                participantWrapperPayload.getFrom(),
                participantWrapperPayload.getTo());
        List<String> participantIds = getParticipantIdsFromElasticList(participantsEsDataWithinRange);
        participants = Participant.getParticipantsByIds(ddpInstance.getName(), participantIds);
        if (ddpInstance.isHasRole()) {
            medicalRecords = MedicalRecord.getMedicalRecordsByParticipantIds(ddpInstance.getName(), participantIds);
            oncHistoryDetails = OncHistoryDetail.getOncHistoryDetailsByParticipantIds(ddpInstance.getName(), participantIds);
        }
        if (DDPInstanceDao.getRole(ddpInstance.getName(), DBConstants.KIT_REQUEST_ACTIVATED)) {
            kitRequests = KitRequestShipping.getKitRequestsByParticipantIds(ddpInstance, participantIds);
        }
        abstractionActivities =
                AbstractionActivity.getAllAbstractionActivityByParticipantIds(ddpInstance.getName(), participantIds);
        abstractionSummary = AbstractionFinal.getAbstractionFinalByParticipantIds(ddpInstance.getName(), participantIds);
        Map<String, List<String>> proxiesIdsFromElasticList = getProxiesIdsFromElasticList(participantsEsDataWithinRange);
        proxiesByParticipantIds =
                getProxiesWithParticipantIdsByProxiesIds(ddpInstance.getParticipantIndexES(), proxiesIdsFromElasticList);
        participantDataByParticipantIds = new ParticipantDataDao().getParticipantDataByParticipantIds(participantIds);
    }

    private List<ParticipantWrapperDto> collectData() {
        List<ParticipantWrapperDto> result = new ArrayList<>();
        for (ElasticSearch elasticSearch: participantsEsDataWithinRange) {
            String participantId = elasticSearch.getParticipantIdFromProfile();
            if (StringUtils.isBlank(participantId)) continue;
            Participant participant = participants.stream()
                    .filter(ppt -> participantId.equals(ppt.getDdpParticipantId()))
                    .findFirst()
                    .orElse(null);
            result.add(new ParticipantWrapperDto(
                    elasticSearch, participant, medicalRecords.get(participantId),
                    oncHistoryDetails.get(participantId), kitRequests.get(participantId), abstractionActivities.get(participantId),
                    abstractionSummary.get(participantId), proxiesByParticipantIds.get(participantId), participantDataByParticipantIds.get(participantId)));
        }
        return result;
    }

    private void sortBySelfElseById(Collection<List<ParticipantDataDto>> participantDatas) {
        participantDatas.forEach(pDataList -> pDataList.sort((o1, o2) -> {
            Map<String, String> pData = new Gson().fromJson(o1.getData().orElse(""), new TypeToken<Map<String, String>>() {}.getType());
            if (Objects.nonNull(pData) && FamilyMemberConstants.MEMBER_TYPE_SELF.equals(pData.get(FamilyMemberConstants.MEMBER_TYPE))) return -1;
            return o1.getParticipantDataId() - o2.getParticipantDataId();
        }));
    }

    public static Map<String, Map<String, Object>> getParticipantESDataConsideringNumberOfParameters(@NonNull DDPInstance instance, Map<String, String> filters, String source) {
        Map<String, Map<String, Object>> participantESData;
        String wholeFilter = filters.get(source);
        String[] betweenAnds = wholeFilter.split(Filter.AND_TRIMMED);
        boolean tooManyParameters = false;
        participantESData = new HashMap<>();
        String lessThanLimit = Arrays.stream(betweenAnds).filter(query -> query.split(Filter.OR_TRIMMED).length <= Filter.THOUSAND).collect(Collectors.joining(Filter.AND));
        for (String query: betweenAnds) {
            String[] orQueries = query.split(Filter.OR_TRIMMED);
            if (orQueries.length <= Filter.THOUSAND) {
                continue;
            }
            tooManyParameters = true;
            Collection<List<String>> partitionBaseList = partitionBasedOnSize(Arrays.asList(orQueries), Filter.THOUSAND);
            for (Iterator i = partitionBaseList.iterator(); i.hasNext();) {
                List<String> baseListPart = ((List<String>) i.next());
                Map<String, Map<String, Object>> tempParticipantESData = ElasticSearchUtil
                        .getFilteredDDPParticipantsFromES(instance, lessThanLimit + Filter.AND + String.join(Filter.OR, baseListPart));
                if (tempParticipantESData != null) {
                    participantESData.putAll(tempParticipantESData);
                }
            }
            break;
        }
        if (!tooManyParameters) {
            participantESData = ElasticSearchUtil.getFilteredDDPParticipantsFromES(instance, filters.get(source));
        }
        return participantESData;
    }

    private static <String> Collection<List<String>> partitionBasedOnSize(List<String> inputList, int size) {
        final AtomicInteger counter = new AtomicInteger(0);
        return inputList.stream()
                .collect(Collectors.groupingBy(s -> counter.getAndIncrement()/size))
                .values();
    }

    private static Map<String, Map<String, Object>> addParticipantESData(DDPInstance instance, List<String> baseList, Map<String, Map<String, Object>> participantESData, String andCommand, String orCommand) {
        String filterLegacy = Arrays.stream(baseList.toArray(new String[0])).collect(Collectors.joining(orCommand));
        Map<String, Map<String, Object>> tmp = ElasticSearchUtil.getFilteredDDPParticipantsFromES(instance, andCommand + filterLegacy);
        return Stream.of(participantESData, tmp).flatMap(m -> m.entrySet().stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private static void sortBySelfElseById(Map<String, List<ParticipantData>> participantData) {
        participantData.values().forEach(pDataList -> pDataList.sort((o1, o2) -> {
            Map<String, String> pData = new Gson().fromJson(o1.getData(), new TypeToken<Map<String, String>>() {}.getType());
            if (Objects.nonNull(pData) && FamilyMemberConstants.MEMBER_TYPE_SELF.equals(pData.get(FamilyMemberConstants.MEMBER_TYPE))) return -1;
            return Integer.parseInt(o1.getDataId()) - Integer.parseInt(o2.getDataId());
        }));
    }

    //TODO
    public static Optional<ParticipantWrapperDto> getParticipantFromESByHruid(DDPInstance ddpInstanceByRealm, String participantHruid) {
        Map<String, String> queryConditions = new HashMap<>();
        queryConditions.put("ES", ElasticSearchUtil.BY_HRUID + "'" + participantHruid + "'");
//        List<ParticipantWrapperDto> participantsBelongToRealm = ParticipantWrapper.getFilteredList(ddpInstanceByRealm, queryConditions);
//        return participantsBelongToRealm.stream().filter(Objects::nonNull).findFirst();
        return Optional.empty();
    }

    //TODO
    public static Optional<ParticipantWrapperDto> getParticipantFromESByLegacyShortId(DDPInstance ddpInstanceByRealm, String participantLegacyShortId) {
        Map<String, String> queryConditions = new HashMap<>();
        queryConditions.put("ES", ElasticSearchUtil.BY_LEGACY_SHORTID + "'" + participantLegacyShortId + "'");
//        List<ParticipantWrapperDto> participantsBelongToRealm = ParticipantWrapper.getFilteredList(ddpInstanceByRealm, queryConditions);
//        return participantsBelongToRealm.stream().filter(Objects::nonNull).findFirst();
        return Optional.empty();
    }

    public static String getParticipantGuid(Optional<ParticipantWrapperDto> participantWrapper) {
        if (Objects.isNull(participantWrapper)) return "";
        return participantWrapper
                .map(p -> p.getEsData().getProfile().orElse(new ESProfile()).getParticipantGuid())
                .orElse("");
    }

    public static String getParticipantLegacyAltPid(Optional<ParticipantWrapperDto> maybeParticipant) {
        return maybeParticipant
                .map(p -> p.getEsData().getProfile().orElse(new ESProfile()).getParticipantLegacyAltPid())
                .orElse("");
    }

    public static Map<String, Map<String, Object>> getESData(@NonNull DDPInstance instance) {
        if (StringUtils.isNotBlank(instance.getParticipantIndexES())) {
            return ElasticSearchUtil.getDDPParticipantsFromES(instance.getName(), instance.getParticipantIndexES());
        }
        return null;
    }

//    public Optional<ElasticSearch> getESData1(String participantEsIndex, int from, int to) {
//        if (StringUtils.isBlank(participantEsIndex)) throw new IllegalArgumentException("participant es index cannot be empty");
//        if (to <= 0) throw new IllegalArgumentException("incorrect from/to range");
//        return ElasticSearchUtil.getDDPParticipantsFromES(instance.getName(), instance.getParticipantIndexES());
//    }

    public static Map<String, Map<String, Object>> getProxyData(@NonNull DDPInstance instance) {
        if (StringUtils.isNotBlank(instance.getUsersIndexES())) {
            return ElasticSearchUtil.getDDPParticipantsFromES(instance.getName(), instance.getUsersIndexES());
        }
        return null;
    }

    private static List<String> getCommonEntries(List<String> list1, List<String> list2) {
        if (list1 == null) {
            return list2;
        }
        else {
            list1.retainAll(list2);
            return list1;
        }
    }


    public static List<Map<String, Object>> getProxyProfiles(Map<String, Object> participantData, Map<String, Map<String, Object>> proxyDataES) {
        if (participantData != null && !participantData.isEmpty() && proxyDataES != null && !proxyDataES.isEmpty()) {
            List<String> proxies = (List<String>) participantData.get("proxies");
            List<Map<String, Object>> proxyData = new ArrayList<>();
            if (proxies != null && !proxies.isEmpty()) {
                proxies.forEach(proxy -> {
                    Map<String, Object> proxyD = proxyDataES.get(proxy);
                    if (proxyD != null) {
                        proxyData.add(proxyD);
                    }
                });
                return proxyData;
            }
        }
        return null;
    }

    public List<String> getParticipantIdsFromElasticList(List<ElasticSearch> elasticSearchList) {
        return elasticSearchList.
                stream()
                .flatMap(elasticSearch -> elasticSearch.getProfile().stream())
                .map(ESProfile::getParticipantGuid)
                .collect(Collectors.toList());
    }

    public Map<String, List<String>> getProxiesIdsFromElasticList(List<ElasticSearch> elasticSearchList) {
        Map<String, List<String>> participantsWithProxies = new HashMap<>();
        elasticSearchList.stream()
                .filter(elasticSearch -> elasticSearch.getProxies().orElse(Collections.emptyList()).size() > 0)
                .forEach(elasticSearch -> participantsWithProxies.put(elasticSearch.getParticipantIdFromProfile(), elasticSearch.getProxies().get()));
        return participantsWithProxies;
    }

    public Map<String, List<ElasticSearch>> getProxiesWithParticipantIdsByProxiesIds(String participantIndexES,
                                                                                     Map<String, List<String>> proxiesIdsByParticipantIds) {
        Map<String, List<ElasticSearch>> proxiesByParticipantIds = new HashMap<>();
        List<String> proxiesIds = proxiesIdsByParticipantIds.values().stream().flatMap(Collection::stream).collect(Collectors.toList());
        List<ElasticSearch> participantsByIds = elasticSearchable.getParticipantsByIds(participantIndexES, proxiesIds);
        participantsByIds.forEach(elasticSearch -> {
            String proxyId = elasticSearch.getParticipantIdFromProfile();
            for (Map.Entry<String, List<String>> entry: proxiesIdsByParticipantIds.entrySet()) {
                String participantId = entry.getKey();
                List<String> proxies = entry.getValue();
                if (proxies.contains(proxyId)) {
                    proxiesByParticipantIds.merge(participantId, new ArrayList<>(List.of(elasticSearch)), (prev, curr) -> {
                        prev.addAll(curr);
                        return prev;
                    });
                    break;
                }

            }
        });
        return proxiesByParticipantIds;
    }
}
