package org.broadinstitute.dsm.util;

import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.lucene.search.join.ScoreMode;
import org.broadinstitute.ddp.db.TransactionWrapper;
import org.broadinstitute.ddp.handlers.util.MedicalInfo;
import org.broadinstitute.dsm.db.DDPInstance;
import org.broadinstitute.dsm.model.Filter;
import org.broadinstitute.dsm.model.ddp.DDPParticipant;
import org.broadinstitute.dsm.statics.ApplicationConfigConstants;
import org.broadinstitute.dsm.statics.DBConstants;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

public class ElasticSearchUtil {

    private static final Logger logger = LoggerFactory.getLogger(DDPRequestUtil.class);

    private static final String ACTIVITIES = "activities";
    private static final String ACTIVITIES_QUESTIONS_ANSWER = "activities.questionsAnswers";
    private static final String ACTIVITIES_QUESTIONS_ANSWER_ANSWER = "activities.questionsAnswers.answer";
    private static final String ACTIVITIES_QUESTIONS_ANSWER_DATE = "activities.questionsAnswers.date";
    private static final String ACTIVITIES_QUESTIONS_ANSWER_STABLE_ID = "activities.questionsAnswers.stableId";
    private static final String PROFILE = "profile";
    private static final String DATA = "data";
    private static final String ACTIVITY_CODE = "activityCode";
    private static final String ADDRESS = "address";

    public static RestHighLevelClient getClientForElasticsearchCloud(@NonNull String baseUrl, @NonNull String userName, @NonNull String password) throws MalformedURLException {
        final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(userName, password));

        URL url = new URL(baseUrl);

        RestClientBuilder builder = RestClient.builder(new HttpHost(url.getHost(), url.getPort(), url.getProtocol()))
                .setHttpClientConfigCallback(httpClientBuilder -> httpClientBuilder
                        .setDefaultCredentialsProvider(credentialsProvider))
                .setMaxRetryTimeoutMillis(100000);

        return new RestHighLevelClient(builder);
    }

    public static Map<String, Map<String, Object>> getDDPParticipantsFromES(@NonNull String realm, @NonNull String index) {
        Map<String, Map<String, Object>> esData = new HashMap<>();
        if (StringUtils.isNotBlank(index)) {
            logger.info("Collecting ES data");
            try {
                try (RestHighLevelClient client = getClientForElasticsearchCloud(TransactionWrapper.getSqlFromConfig(ApplicationConfigConstants.ES_URL),
                        TransactionWrapper.getSqlFromConfig(ApplicationConfigConstants.ES_USERNAME), TransactionWrapper.getSqlFromConfig(ApplicationConfigConstants.ES_PASSWORD))) {
                    int scrollSize = 1000;
                    SearchRequest searchRequest = new SearchRequest(index);
                    SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
                    SearchResponse response = null;
                    int i = 0;
                    while (response == null || response.getHits().getHits().length != 0) {
                        searchSourceBuilder.query(QueryBuilders.matchAllQuery());
                        searchSourceBuilder.size(scrollSize);
                        searchSourceBuilder.from(i * scrollSize);
                        searchRequest.source(searchSourceBuilder);

                        response = client.search(searchRequest, RequestOptions.DEFAULT);
                        addingParticipantStructuredHits(response, esData, realm);
                        i++;
                    }
                }
            }
            catch (Exception e) {
                throw new RuntimeException("Couldn't get participants from ES for instance " + realm, e);
            }
            logger.info("Got " + esData.size() + " participants from ES for instance " + realm);
        }
        return esData;
    }

    public static Map<String, Map<String, Object>> getFilteredDDPParticipantsFromES(@NonNull DDPInstance instance, @NonNull String filter) {
        String index = instance.getParticipantIndexES();
        if (StringUtils.isNotBlank(index)) {
            Map<String, Map<String, Object>> esData = new HashMap<>();
            logger.info("Collecting ES data");
            try {
                try (RestHighLevelClient client = getClientForElasticsearchCloud(TransactionWrapper.getSqlFromConfig(ApplicationConfigConstants.ES_URL),
                        TransactionWrapper.getSqlFromConfig(ApplicationConfigConstants.ES_USERNAME), TransactionWrapper.getSqlFromConfig(ApplicationConfigConstants.ES_PASSWORD))) {
                    int scrollSize = 1000;
                    SearchRequest searchRequest = new SearchRequest(index);
                    SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
                    SearchResponse response = null;
                    int i = 0;
                    while (response == null || response.getHits().getHits().length != 0) {
                        AbstractQueryBuilder query = createESQuery(filter);
                        if (query == null) {
                            throw new RuntimeException("Couldn't create query from filter " + filter);
                        }
                        searchSourceBuilder.query(query);
                        searchSourceBuilder.size(scrollSize);
                        searchSourceBuilder.from(i * scrollSize);
                        searchRequest.source(searchSourceBuilder);

                        response = client.search(searchRequest, RequestOptions.DEFAULT);
                        addingParticipantStructuredHits(response, esData, instance.getName());
                        i++;
                    }
                }
            }
            catch (Exception e) {
                throw new RuntimeException("Couldn't get participants from ES for instance " + instance.getName(), e);
            }
            logger.info("Got " + esData.size() + " participants from ES for instance " + instance.getName());
            return esData;
        }
        return null;
    }

    public static DDPParticipant getParticipantAsDDPParticipant(@NonNull Map<String, Map<String, Object>> participantsESData, @NonNull String ddpParticipantId) {
        if (participantsESData != null && !participantsESData.isEmpty()) {
            Map<String, Object> participantESData = participantsESData.get(ddpParticipantId);
            if (participantESData != null && !participantESData.isEmpty()) {
                Map<String, Object> address = (Map<String, Object>) participantESData.get("address");
                Map<String, Object> profile = (Map<String, Object>) participantESData.get("profile");
                if (address != null && !address.isEmpty() && profile != null && !profile.isEmpty()) {
                    return new DDPParticipant(ddpParticipantId, "", (String) address.get("mailToName"),
                            (String) address.get("country"), (String) address.get("city"), (String) address.get("zip"),
                            (String) address.get("street1"), (String) address.get("street2"), (String) address.get("state"),
                            (String) profile.get("hruid"), null);
                }
                else if (profile != null && !profile.isEmpty()) {
                    return new DDPParticipant((String) profile.get("hruid"), "", (String) profile.get("firstName"), (String) profile.get("lastName"));
                }
            }
        }
        return null;
    }

    public static MedicalInfo getParticipantAsMedicalInfo(@NonNull Map<String, Map<String, Object>> participantsESData, @NonNull String ddpParticipantId) {
        if (participantsESData != null && !participantsESData.isEmpty()) {
            Map<String, Object> participantESData = participantsESData.get(ddpParticipantId);
            if (participantESData != null && !participantESData.isEmpty()) {
                Map<String, Object> dsm = (Map<String, Object>) participantESData.get("dsm");
                if (dsm != null && !dsm.isEmpty()) {
                    boolean hasConsentedToBloodDraw = (boolean) dsm.get("hasConsentedToBloodDraw");
                    boolean hasConsentedToTissueSample = (boolean) dsm.get("hasConsentedToTissueSample");
                    MedicalInfo medicalInfo = new MedicalInfo(ddpParticipantId);
                    medicalInfo.setDateOfDiagnosis(dsm.get("diagnosisMonth") + "/" + dsm.get("diagnosisYear"));
                    medicalInfo.setDob((String) dsm.get("dateOfBirth"));
                    medicalInfo.setDrawBloodConsent(hasConsentedToBloodDraw ? 1 : 0);
                    medicalInfo.setTissueSampleConsent(hasConsentedToTissueSample ? 1 : 0);
                    return medicalInfo;
                }
            }
        }
        return null;
    }

    private static AbstractQueryBuilder<? extends AbstractQueryBuilder<?>> createESQuery(@NonNull String filter) {
        String[] filters = filter.split(Filter.AND);
        BoolQueryBuilder finalQuery = new BoolQueryBuilder();

        for (String f : filters) {
            if (StringUtils.isNotBlank(f) && f.contains(DBConstants.ALIAS_DELIMITER)) {
                if (f.contains(Filter.EQUALS) || f.contains(Filter.LIKE)) {
                    boolean wildCard = false;
                    f = f.replace("(", "").replace(")", "").trim();
                    String[] nameValue = f.split(Filter.EQUALS);
                    if (nameValue.length == 1) { //didn't contain EQUALS -> split LIKE
                        nameValue = f.split(Filter.LIKE);
                        wildCard = true;
                    }
                    String userEntered = nameValue[1].replaceAll("'", "").trim();
                    if (wildCard) {
                        userEntered = userEntered.replaceAll("%", "").trim();
                    }
                    if (StringUtils.isNotBlank(nameValue[0]) && (nameValue[0].startsWith(PROFILE) || nameValue[0].startsWith(ADDRESS))) {
                        if (nameValue[0].contains("createdAt")) {
                            try {
                                long start = SystemUtil.getLongFromString(userEntered);
                                //set endDate to midnight of that date
                                String endDate = userEntered + " 23:59:59";
                                long end = SystemUtil.getLongFromDetailDateString(endDate);
                                finalQuery.must(QueryBuilders.rangeQuery("createdAt").from(start).to(end));
                            }
                            catch (ParseException e) {
                                //was no date string so go for normal text
                                String[] dataParam = nameValue[0].split("\\.");
                                if (wildCard) {
                                    finalQuery.must(QueryBuilders.wildcardQuery(dataParam[1].trim(), userEntered + "*"));
                                }
                                else {
                                    finalQuery.must(QueryBuilders.matchQuery(dataParam[1], userEntered));
                                }
                            }
                        }
                        else {
                            if (wildCard) {
                                finalQuery.must(QueryBuilders.wildcardQuery(nameValue[0].trim(), userEntered + "*"));
                            }
                            else {
                                finalQuery.must(QueryBuilders.matchQuery(nameValue[0], userEntered));
                            }
                        }
                    }
                    else {
                        if (StringUtils.isNotBlank(nameValue[0]) && nameValue[0].startsWith(DATA)) {
                            String[] dataParam = nameValue[0].split("\\.");
                            if (wildCard) {
                                finalQuery.must(QueryBuilders.wildcardQuery(dataParam[1].trim(), userEntered + "*"));
                            }
                            else {
                                finalQuery.must(QueryBuilders.matchQuery(dataParam[1], userEntered));
                            }

                        }
                        else {
                            String[] surveyParam = nameValue[0].split("\\.");
                            BoolQueryBuilder activityAnswer = new BoolQueryBuilder();
                            BoolQueryBuilder queryBuilder = new BoolQueryBuilder();

                            if ("createdAt".equals(surveyParam[1]) || "completedAt".equals(surveyParam[1]) || "lastUpdatedAt".equals(surveyParam[1]) || "status".equals(surveyParam[1])) {
                                try {
                                    //activity dates
                                    long start = SystemUtil.getLongFromString(userEntered);
                                    //set endDate to midnight of that date
                                    String endDate = userEntered + " 23:59:59";
                                    long end = SystemUtil.getLongFromDetailDateString(endDate);
                                    queryBuilder.must(QueryBuilders.rangeQuery(ACTIVITIES + DBConstants.ALIAS_DELIMITER + surveyParam[1]).from(start).to(end));
                                }
                                catch (ParseException e) {
                                    //activity status
                                    if (wildCard) {
                                        queryBuilder.must(QueryBuilders.wildcardQuery(ACTIVITIES + DBConstants.ALIAS_DELIMITER + surveyParam[1].trim(), userEntered + "*"));
                                    }
                                    else {
                                        queryBuilder.must(QueryBuilders.matchQuery(ACTIVITIES + DBConstants.ALIAS_DELIMITER + surveyParam[1], userEntered));
                                    }
                                }
                            }
                            else {
                                //activity user entered
                                activityAnswer.must(QueryBuilders.matchQuery(ACTIVITIES_QUESTIONS_ANSWER_STABLE_ID, surveyParam[1]).operator(Operator.AND));
                                try {
                                    SystemUtil.getLongFromString(userEntered);
                                    activityAnswer.must(QueryBuilders.matchQuery(ACTIVITIES_QUESTIONS_ANSWER_DATE, userEntered).operator(Operator.AND));
                                }
                                catch (ParseException e) {
                                    //was no date string so go for normal text
                                    if (wildCard) {
                                        activityAnswer.must(QueryBuilders.wildcardQuery(ACTIVITIES_QUESTIONS_ANSWER_ANSWER, userEntered + "*"));
                                    }
                                    else {
                                        activityAnswer.must(QueryBuilders.matchQuery(ACTIVITIES_QUESTIONS_ANSWER_ANSWER, userEntered).operator(Operator.AND));
                                    }
                                }
                                NestedQueryBuilder queryActivityAnswer = QueryBuilders.nestedQuery(ACTIVITIES_QUESTIONS_ANSWER, activityAnswer, ScoreMode.Avg);
                                queryBuilder.must(queryActivityAnswer);
                            }

                            queryBuilder.must(QueryBuilders.matchQuery(ACTIVITIES + DBConstants.ALIAS_DELIMITER + ACTIVITY_CODE, surveyParam[0]).operator(Operator.AND));
                            NestedQueryBuilder query = QueryBuilders.nestedQuery(ACTIVITIES, queryBuilder, ScoreMode.Avg);
                            finalQuery.must(query);
                        }
                    }
                }
                else if (f.contains(Filter.LARGER_EQUALS)) {
                    String[] nameValue = f.split(Filter.LARGER_EQUALS);
                    String userEntered = nameValue[1].replaceAll("'", "").trim();
                    try {
                        long start = SystemUtil.getLongFromString(userEntered);
                        finalQuery.must(QueryBuilders.rangeQuery("statusTimestamp").from(start));
                    }
                    catch (ParseException e) {
                        //was no date string so go for normal text
                        String[] dataParam = nameValue[0].split("\\.");
                        finalQuery.must(QueryBuilders.matchQuery(dataParam[1], userEntered));
                    }
                }
                else if (f.contains(Filter.SMALLER_EQUALS)) {
                    String[] nameValue = f.split(Filter.SMALLER_EQUALS);
                    String userEntered = nameValue[1].replaceAll("'", "").trim();
                    try {
                        String endDate = userEntered + " 23:59:59";
                        long end = SystemUtil.getLongFromDetailDateString(endDate);
                        finalQuery.must(QueryBuilders.rangeQuery("statusTimestamp").to(end));
                    }
                    catch (RuntimeException e) {
                        //was no date string so go for normal text
                        String[] dataParam = nameValue[0].split("\\.");
                        finalQuery.must(QueryBuilders.matchQuery(dataParam[1], userEntered));
                    }
                }
                else { //is not null or is null
                    if (f.contains(Filter.IS_NOT_NULL)) {
                        String[] nameValue = f.split(Filter.IS_NOT_NULL);
                        if (StringUtils.isNotBlank(nameValue[0]) && (nameValue[0].startsWith(PROFILE) || nameValue[0].startsWith(ADDRESS))) {
                            ExistsQueryBuilder existsQuery = new ExistsQueryBuilder(nameValue[0].trim());
                            finalQuery.must(existsQuery);
                        }
                        else if (StringUtils.isNotBlank(nameValue[0]) && !nameValue[0].startsWith(DATA)) {
                            String[] surveyParam = nameValue[0].split("\\.");

                            BoolQueryBuilder activityAnswer = new BoolQueryBuilder();
                            ExistsQueryBuilder existsQuery = new ExistsQueryBuilder(ACTIVITIES_QUESTIONS_ANSWER_ANSWER);
                            activityAnswer.must(existsQuery);
                            activityAnswer.must(QueryBuilders.matchQuery(ACTIVITIES_QUESTIONS_ANSWER_STABLE_ID, surveyParam[1].trim()));
                            NestedQueryBuilder queryActivityAnswer = QueryBuilders.nestedQuery(ACTIVITIES_QUESTIONS_ANSWER, activityAnswer, ScoreMode.Avg);

                            BoolQueryBuilder queryBuilder = new BoolQueryBuilder();
                            queryBuilder.must(QueryBuilders.matchQuery(ACTIVITIES + DBConstants.ALIAS_DELIMITER + ACTIVITY_CODE, surveyParam[0].trim()).operator(Operator.AND));
                            queryBuilder.must(queryActivityAnswer);
                            NestedQueryBuilder query = QueryBuilders.nestedQuery(ACTIVITIES, queryBuilder, ScoreMode.Avg);
                            finalQuery.must(query);
                        }
                    }
                    else if (f.contains(Filter.IS_NULL)) {

                    }
                }
            }
        }
        if (finalQuery.hasClauses()) {
            return finalQuery;
        }
        return null;
    }

    public static void addingParticipantStructuredHits(@NonNull SearchResponse response, Map<String, Map<String, Object>> esData, String ddp) {
        for (SearchHit hit : response.getHits()) {
            Map<String, Object> sourceMap = hit.getSourceAsMap();
            sourceMap.put("ddp", ddp);
            String legacyId = (String) ((Map<String, Object>) sourceMap.get(PROFILE)).get("legacyAltPid");
            if (StringUtils.isNotBlank(legacyId)) {
                esData.put(legacyId, sourceMap);
            }
            else {
                esData.put(hit.getId(), sourceMap);
            }
        }
    }

    public static Map<String, Map<String, Object>> getActivityDefinitions(@NonNull DDPInstance instance) {
        Map<String, Map<String, Object>> esData = new HashMap<>();
        String index = instance.getActivityDefinitionIndexES();
        if (StringUtils.isNotBlank(index)) {
            logger.info("Collecting activity definitions from ES");
            try {
                try (RestHighLevelClient client = getClientForElasticsearchCloud(TransactionWrapper.getSqlFromConfig(ApplicationConfigConstants.ES_URL),
                        TransactionWrapper.getSqlFromConfig(ApplicationConfigConstants.ES_USERNAME), TransactionWrapper.getSqlFromConfig(ApplicationConfigConstants.ES_PASSWORD))) {
                    int scrollSize = 1000;

                    SearchRequest searchRequest = new SearchRequest(index);
                    SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
                    SearchResponse response = null;
                    int i = 0;
                    while (response == null || response.getHits().getHits().length != 0) {
                        searchSourceBuilder.query(QueryBuilders.matchAllQuery());
                        searchSourceBuilder.size(scrollSize);
                        searchSourceBuilder.from(i * scrollSize);
                        searchRequest.source(searchSourceBuilder);

                        response = client.search(searchRequest, RequestOptions.DEFAULT);
                        addingActivityDefinitionHits(response, esData);
                        i++;
                    }
                }
            }
            catch (Exception e) {
                throw new RuntimeException("Couldn't get activity definition from ES for instance " + instance.getName(), e);
            }
            logger.info("Got " + esData.size() + " activity definitions from ES for instance " + instance.getName());
        }
        return esData;
    }

    public static void addingActivityDefinitionHits(@NonNull SearchResponse response, Map<String, Map<String, Object>> esData) {
        for (SearchHit hit : response.getHits()) {
            Map<String, Object> sourceMap = hit.getSourceAsMap();
            String activityCode = (String) sourceMap.get(ACTIVITY_CODE);
            if (StringUtils.isNotBlank(activityCode)) {
                esData.put(activityCode, sourceMap);
            }
            else {
                esData.put(hit.getId(), sourceMap);
            }
        }
    }
}
