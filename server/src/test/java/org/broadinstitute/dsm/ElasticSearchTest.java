package org.broadinstitute.dsm;

import org.apache.lucene.search.join.ScoreMode;
import org.broadinstitute.dsm.util.ElasticSearchUtil;
import org.broadinstitute.dsm.util.SystemUtil;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

public class ElasticSearchTest extends TestHelper {

    private static final Logger logger = LoggerFactory.getLogger(RouteTestSample.class);

    @BeforeClass
    public static void before() throws Exception {
        setupDB();
    }

    @Test
    public void testGetRequest() throws Exception {

        try (RestHighLevelClient client = ElasticSearchUtil.getClientForElasticsearchCloud(cfg.getString("elasticSearch.url"), cfg.getString("elasticSearch.username"), cfg.getString("elasticSearch.password"))) {
            GetRequest getRequest = new GetRequest("participants.cmi.angio");
            GetResponse getResponse = null;
            try {
                getResponse = client.get(getRequest, RequestOptions.DEFAULT);
                Map<String, Object> sourceMap = getResponse.getSourceAsMap();

            }
            catch (IOException e) {

            }
        }
    }

    @Test
    public void allParticipants() throws Exception {
        try (RestHighLevelClient client = ElasticSearchUtil.getClientForElasticsearchCloud(cfg.getString("elasticSearch.url"), cfg.getString("elasticSearch.username"), cfg.getString("elasticSearch.password"))) {
            int scrollSize = 1000;
            Map<String, Map<String, Object>> esData = new HashMap<>();
            SearchRequest searchRequest = new SearchRequest("participants_structured.cmi.cmi-osteo");
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            SearchResponse response = null;
            int i = 0;
            while (response == null || response.getHits().getHits().length != 0) {
                searchSourceBuilder.query(QueryBuilders.matchAllQuery());
                searchSourceBuilder.size(scrollSize);
                searchSourceBuilder.from(i * scrollSize);
                searchRequest.source(searchSourceBuilder);

                response = client.search(searchRequest, RequestOptions.DEFAULT);
                ElasticSearchUtil.addingParticipantStructuredHits(response, esData, "realm");
                i++;
            }
            Assert.assertNotEquals(0, esData.size());
        }
    }

    @Test
    public void searchPTByProfileDataLike() throws Exception {
        try (RestHighLevelClient client = ElasticSearchUtil.getClientForElasticsearchCloud(cfg.getString("elasticSearch.url"), cfg.getString("elasticSearch.username"), cfg.getString("elasticSearch.password"))) {
            int scrollSize = 1000;
            Map<String, Map<String, Object>> esData = new HashMap<>();
            SearchRequest searchRequest = new SearchRequest("participants_structured.cmi.cmi-osteo");
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            SearchResponse response = null;
            int i = 0;
            while (response == null || response.getHits().getHits().length != 0) {
                searchSourceBuilder.query(QueryBuilders.wildcardQuery("profile.firstName", "Kiara*"));

                searchSourceBuilder.size(scrollSize);
                searchSourceBuilder.from(i * scrollSize);
                searchRequest.source(searchSourceBuilder);

                response = client.search(searchRequest, RequestOptions.DEFAULT);
                ElasticSearchUtil.addingParticipantStructuredHits(response, esData, "realm");
                i++;
            }
            Assert.assertNotEquals(0, esData.size());
        }
    }

    @Test
    public void activityDefinitionSearchRequest() throws Exception {
        try (RestHighLevelClient client = ElasticSearchUtil.getClientForElasticsearchCloud(cfg.getString("elasticSearch.url"), cfg.getString("elasticSearch.username"), cfg.getString("elasticSearch.password"))) {
            int scrollSize = 1000;
            Map<String, Map<String, Object>> esData = new HashMap<>();
            SearchRequest searchRequest = new SearchRequest("activity_definition.cmi.cmi-osteo");
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            SearchResponse response = null;
            int i = 0;
            while (response == null || response.getHits().getHits().length != 0) {
                searchSourceBuilder.query(QueryBuilders.matchAllQuery());
                searchSourceBuilder.size(scrollSize);
                searchSourceBuilder.from(i * scrollSize);
                searchRequest.source(searchSourceBuilder);

                response = client.search(searchRequest, RequestOptions.DEFAULT);
                ElasticSearchUtil.addingActivityDefinitionHits(response, esData);
                i++;
            }
            Assert.assertNotEquals(0, esData.size());
        }
    }

    @Test
    public void searchPTByActivityDte() throws Exception {
        try (RestHighLevelClient client = ElasticSearchUtil.getClientForElasticsearchCloud(cfg.getString("elasticSearch.url"), cfg.getString("elasticSearch.username"), cfg.getString("elasticSearch.password"))) {
            int scrollSize = 1000;
            Map<String, Map<String, Object>> esData = new HashMap<>();
            SearchRequest searchRequest = new SearchRequest("participants_structured.cmi.cmi-brain");
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            SearchResponse response = null;
            int i = 0;
            while (response == null || response.getHits().getHits().length != 0) {
                BoolQueryBuilder activityAnswer = new BoolQueryBuilder();
                activityAnswer.must(QueryBuilders.matchQuery("activities.questionsAnswers.stableId", "CONSENT_DOB"));
                activityAnswer.must(QueryBuilders.matchQuery("activities.questionsAnswers.date", "2011-11-11"));
                NestedQueryBuilder queryActivityAnswer = QueryBuilders.nestedQuery("activities.questionsAnswers", activityAnswer, ScoreMode.Avg);

                BoolQueryBuilder queryBuilder = new BoolQueryBuilder();
                queryBuilder.must(QueryBuilders.matchQuery("activities.activityCode", "CONSENT").operator(Operator.AND));
                queryBuilder.must(queryActivityAnswer);
                NestedQueryBuilder query = QueryBuilders.nestedQuery("activities", queryBuilder, ScoreMode.Avg);

                searchSourceBuilder.query(query);
                searchSourceBuilder.size(scrollSize);
                searchSourceBuilder.from(i * scrollSize);
                searchRequest.source(searchSourceBuilder);

                response = client.search(searchRequest, RequestOptions.DEFAULT);
                ElasticSearchUtil.addingParticipantStructuredHits(response, esData, "realm");
                i++;
            }
            Assert.assertNotEquals(0, esData.size());
        }
    }

    @Test
    public void searchPTByActivityAnswer() throws Exception {
        try (RestHighLevelClient client = ElasticSearchUtil.getClientForElasticsearchCloud(cfg.getString("elasticSearch.url"), cfg.getString("elasticSearch.username"), cfg.getString("elasticSearch.password"))) {
            int scrollSize = 1000;
            Map<String, Map<String, Object>> esData = new HashMap<>();
            SearchRequest searchRequest = new SearchRequest("participants_structured.cmi.cmi-brain");
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            SearchResponse response = null;
            int i = 0;
            while (response == null || response.getHits().getHits().length != 0) {
                BoolQueryBuilder activityAnswer = new BoolQueryBuilder();
                activityAnswer.must(QueryBuilders.matchQuery("activities.questionsAnswers.stableId", "PREQUAL_SELF_DESCRIBE"));
                activityAnswer.must(QueryBuilders.matchQuery("activities.questionsAnswers.answer", "MAILING_LIST"));
                NestedQueryBuilder queryActivityAnswer = QueryBuilders.nestedQuery("activities.questionsAnswers", activityAnswer, ScoreMode.Avg);

                BoolQueryBuilder queryBuilder = new BoolQueryBuilder();
                queryBuilder.must(QueryBuilders.matchQuery("activities.activityCode", "PREQUAL").operator(Operator.AND));
                queryBuilder.must(queryActivityAnswer);
                NestedQueryBuilder query = QueryBuilders.nestedQuery("activities", queryBuilder, ScoreMode.Avg);

                searchSourceBuilder.query(query);
                searchSourceBuilder.size(scrollSize);
                searchSourceBuilder.from(i * scrollSize);
                searchRequest.source(searchSourceBuilder);

                response = client.search(searchRequest, RequestOptions.DEFAULT);
                ElasticSearchUtil.addingParticipantStructuredHits(response, esData, "realm");
                i++;
            }
            Assert.assertNotEquals(0, esData.size());
        }
    }

    @Test
    public void searchPTByProfileFieldNotEmpty() throws Exception {
        try (RestHighLevelClient client = ElasticSearchUtil.getClientForElasticsearchCloud(cfg.getString("elasticSearch.url"), cfg.getString("elasticSearch.username"), cfg.getString("elasticSearch.password"))) {
            int scrollSize = 1000;
            Map<String, Map<String, Object>> esData = new HashMap<>();
            SearchRequest searchRequest = new SearchRequest("participants_structured.cmi.cmi-brain");
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            SearchResponse response = null;
            int i = 0;
            while (response == null || response.getHits().getHits().length != 0) {
                BoolQueryBuilder query = new BoolQueryBuilder();
                ExistsQueryBuilder existsQuery = new ExistsQueryBuilder("profile.lastName");
                query.must(existsQuery);

                searchSourceBuilder.query(query);
                searchSourceBuilder.size(scrollSize);
                searchSourceBuilder.from(i * scrollSize);
                searchRequest.source(searchSourceBuilder);

                response = client.search(searchRequest, RequestOptions.DEFAULT);
                ElasticSearchUtil.addingParticipantStructuredHits(response, esData, "realm");
                i++;
            }
            Assert.assertNotEquals(0, esData.size());
        }
    }

    @Test
    public void searchPTByProfileFieldEmpty() throws Exception { //TODO Simone - not working yet
        try (RestHighLevelClient client = ElasticSearchUtil.getClientForElasticsearchCloud(cfg.getString("elasticSearch.url"), cfg.getString("elasticSearch.username"), cfg.getString("elasticSearch.password"))) {
            int scrollSize = 1000;
            Map<String, Map<String, Object>> esData = new HashMap<>();
            SearchRequest searchRequest = new SearchRequest("participants_structured.cmi.cmi-brain");
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            SearchResponse response = null;
            int i = 0;
            while (response == null || response.getHits().getHits().length != 0) {
                BoolQueryBuilder query = new BoolQueryBuilder();
                ExistsQueryBuilder existsQuery = new ExistsQueryBuilder("profile.lastName");
                query.mustNot(existsQuery);

                searchSourceBuilder.query(query);
                searchSourceBuilder.size(scrollSize);
                searchSourceBuilder.from(i * scrollSize);
                searchRequest.source(searchSourceBuilder);

                response = client.search(searchRequest, RequestOptions.DEFAULT);
                ElasticSearchUtil.addingParticipantStructuredHits(response, esData, "realm");
                i++;
            }
            Assert.assertNotEquals(0, esData.size());
        }
    }

    @Test
    public void searchPTByNotEmptyField() throws Exception {
        try (RestHighLevelClient client = ElasticSearchUtil.getClientForElasticsearchCloud(cfg.getString("elasticSearch.url"), cfg.getString("elasticSearch.username"), cfg.getString("elasticSearch.password"))) {
            int scrollSize = 1000;
            Map<String, Map<String, Object>> esData = new HashMap<>();
            SearchRequest searchRequest = new SearchRequest("participants_structured.cmi.cmi-brain");
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            SearchResponse response = null;
            int i = 0;
            while (response == null || response.getHits().getHits().length != 0) {
                BoolQueryBuilder activityAnswer = new BoolQueryBuilder();
                ExistsQueryBuilder existsQuery = new ExistsQueryBuilder("activities.questionsAnswers.answer");
                activityAnswer.must(existsQuery);
                activityAnswer.must(QueryBuilders.matchQuery("activities.questionsAnswers.stableId", "SURGICAL_PROCEDURES"));
                NestedQueryBuilder queryActivityAnswer = QueryBuilders.nestedQuery("activities.questionsAnswers", activityAnswer, ScoreMode.Avg);

                BoolQueryBuilder queryBuilder = new BoolQueryBuilder();
                queryBuilder.must(QueryBuilders.matchQuery("activities.activityCode", "POSTCONSENT").operator(Operator.AND));
                queryBuilder.must(queryActivityAnswer);
                NestedQueryBuilder query = QueryBuilders.nestedQuery("activities", queryBuilder, ScoreMode.Avg);

                searchSourceBuilder.query(query);
                searchSourceBuilder.size(scrollSize);
                searchSourceBuilder.from(i * scrollSize);
                searchRequest.source(searchSourceBuilder);

                response = client.search(searchRequest, RequestOptions.DEFAULT);
                ElasticSearchUtil.addingParticipantStructuredHits(response, esData, "realm");
                i++;
            }
            Assert.assertNotEquals(0, esData.size());
        }
    }

    @Test
    public void searchPTByCompositeNotEmpty() throws Exception {
        try (RestHighLevelClient client = ElasticSearchUtil.getClientForElasticsearchCloud(cfg.getString("elasticSearch.url"), cfg.getString("elasticSearch.username"), cfg.getString("elasticSearch.password"))) {
            int scrollSize = 1000;
            Map<String, Map<String, Object>> esData = new HashMap<>();
            SearchRequest searchRequest = new SearchRequest("participants_structured.cmi.angio");
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            SearchResponse response = null;
            int i = 0;
            while (response == null || response.getHits().getHits().length != 0) {
                BoolQueryBuilder activityAnswer = new BoolQueryBuilder();
                ExistsQueryBuilder existsQuery = new ExistsQueryBuilder("activities.questionsAnswers.answer");
                activityAnswer.must(existsQuery);
                activityAnswer.must(QueryBuilders.matchQuery("activities.questionsAnswers.stableId", "PHYSICIAN"));
                NestedQueryBuilder queryActivityAnswer = QueryBuilders.nestedQuery("activities.questionsAnswers", activityAnswer, ScoreMode.Avg);

                BoolQueryBuilder queryBuilder = new BoolQueryBuilder();
                queryBuilder.must(QueryBuilders.matchQuery("activities.activityCode", "ANGIORELEASE").operator(Operator.AND));
                queryBuilder.must(queryActivityAnswer);
                NestedQueryBuilder query = QueryBuilders.nestedQuery("activities", queryBuilder, ScoreMode.Avg);

                searchSourceBuilder.query(query);
                searchSourceBuilder.size(scrollSize);
                searchSourceBuilder.from(i * scrollSize);
                searchRequest.source(searchSourceBuilder);

                response = client.search(searchRequest, RequestOptions.DEFAULT);
                ElasticSearchUtil.addingParticipantStructuredHits(response, esData, "realm");
                i++;
            }
            Assert.assertNotEquals(0, esData.size());
        }
    }

    @Test
    public void searchPTByEmptyField() throws Exception { //TODO Simone - not working yet
        try (RestHighLevelClient client = ElasticSearchUtil.getClientForElasticsearchCloud(cfg.getString("elasticSearch.url"), cfg.getString("elasticSearch.username"), cfg.getString("elasticSearch.password"))) {
            int scrollSize = 1000;
            Map<String, Map<String, Object>> esData = new HashMap<>();
            SearchRequest searchRequest = new SearchRequest("participants_structured.cmi.cmi-brain");
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            SearchResponse response = null;
            int i = 0;
            while (response == null || response.getHits().getHits().length != 0) {
                BoolQueryBuilder activityAnswer = new BoolQueryBuilder();
                ExistsQueryBuilder existsQuery = new ExistsQueryBuilder("activities.questionsAnswers.answer");
                activityAnswer.mustNot(existsQuery);
                activityAnswer.mustNot(QueryBuilders.matchQuery("activities.questionsAnswers.stableId", "SURGICAL_PROCEDURES"));
                NestedQueryBuilder queryActivityAnswer = QueryBuilders.nestedQuery("activities.questionsAnswers", activityAnswer, ScoreMode.Avg);

                BoolQueryBuilder queryBuilder = new BoolQueryBuilder();
                queryBuilder.must(QueryBuilders.matchQuery("activities.activityCode", "POSTCONSENT").operator(Operator.AND));
                queryBuilder.must(queryActivityAnswer);
                NestedQueryBuilder query = QueryBuilders.nestedQuery("activities", queryBuilder, ScoreMode.Avg);

                searchSourceBuilder.query(query);
                searchSourceBuilder.size(scrollSize);
                searchSourceBuilder.from(i * scrollSize);
                searchRequest.source(searchSourceBuilder);

                response = client.search(searchRequest, RequestOptions.DEFAULT);
                ElasticSearchUtil.addingParticipantStructuredHits(response, esData, "realm");
                i++;
            }
            Assert.assertNotEquals(0, esData.size());
        }
    }

    @Test
    public void searchPTByProfileData() throws Exception {
        try (RestHighLevelClient client = ElasticSearchUtil.getClientForElasticsearchCloud(cfg.getString("elasticSearch.url"), cfg.getString("elasticSearch.username"), cfg.getString("elasticSearch.password"))) {
            int scrollSize = 1000;
            Map<String, Map<String, Object>> esData = new HashMap<>();
            SearchRequest searchRequest = new SearchRequest("participants_structured.cmi.angio");
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            SearchResponse response = null;
            int i = 0;
            while (response == null || response.getHits().getHits().length != 0) {
                //                searchSourceBuilder.query(QueryBuilders.matchQuery("profile.hruid", "P694FH")); //works! (cmi-brain)
                //                searchSourceBuilder.query(QueryBuilders.matchQuery("profile.firstName", "foo")); //works!
                searchSourceBuilder.query(QueryBuilders.matchQuery("profile.guid", "DT1QLG4VTH4GIKPYTYRN")); //works!

                searchSourceBuilder.size(scrollSize);
                searchSourceBuilder.from(i * scrollSize);
                searchRequest.source(searchSourceBuilder);

                response = client.search(searchRequest, RequestOptions.DEFAULT);
                ElasticSearchUtil.addingParticipantStructuredHits(response, esData, "realm");
                i++;
            }
            Assert.assertNotEquals(0, esData.size());
        }
    }

    @Test
    public void searchPTByTimestamp() throws Exception {
        try (RestHighLevelClient client = ElasticSearchUtil.getClientForElasticsearchCloud(cfg.getString("elasticSearch.url"), cfg.getString("elasticSearch.username"), cfg.getString("elasticSearch.password"))) {
            int scrollSize = 1000;
            Map<String, Map<String, Object>> esData = new HashMap<>();
            SearchRequest searchRequest = new SearchRequest("participants_structured.cmi.cmi-osteo");
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            String dateUserEntered = "2020-01-28";

            final long start = SystemUtil.getLongFromDateString(dateUserEntered);
            //set endDate to midnight of that date
            String endDate = dateUserEntered + " 23:59:59";
            final long end = SystemUtil.getLongFromDetailDateString(endDate);
            SearchResponse response = null;
            int i = 0;
            while (response == null || response.getHits().getHits().length != 0) {
                searchSourceBuilder.query(QueryBuilders.rangeQuery("profile.createdAt").gte(start).lte(end));
                searchSourceBuilder.size(scrollSize);
                searchSourceBuilder.from(i * scrollSize);
                searchRequest.source(searchSourceBuilder);

                response = client.search(searchRequest, RequestOptions.DEFAULT);
                ElasticSearchUtil.addingParticipantStructuredHits(response, esData, "realm");
                i++;
            }
            Assert.assertNotEquals(0, esData.size());
        }
    }

    @Test
    public void searchPTByDateString() throws Exception {
        try (RestHighLevelClient client = ElasticSearchUtil.getClientForElasticsearchCloud(cfg.getString("elasticSearch.url"), cfg.getString("elasticSearch.username"), cfg.getString("elasticSearch.password"))) {
            int scrollSize = 1000;
            Map<String, Map<String, Object>> esData = new HashMap<>();
            SearchRequest searchRequest = new SearchRequest("participants_structured.cmi.cmi-osteo");
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            String date1 = "2002-01-28";
            String date2 = "2002-01-29";
            SearchResponse response = null;
            int i = 0;
            while (response == null || response.getHits().getHits().length != 0) {
                searchSourceBuilder.query(QueryBuilders.rangeQuery("dsm.dateOfBirth").gte(date1).lte(date2));
                searchSourceBuilder.size(scrollSize);
                searchSourceBuilder.from(i * scrollSize);
                searchRequest.source(searchSourceBuilder);

                response = client.search(searchRequest, RequestOptions.DEFAULT);
                ElasticSearchUtil.addingParticipantStructuredHits(response, esData, "realm");
                i++;
            }
            Assert.assertNotEquals(0, esData.size());
        }
    }

    @Test
    public void searchPTAgeUp6Month() throws Exception {
        try (RestHighLevelClient client = ElasticSearchUtil.getClientForElasticsearchCloud(cfg.getString("elasticSearch.url"), cfg.getString("elasticSearch.username"), cfg.getString("elasticSearch.password"))) {
            int scrollSize = 1000;
            Map<String, Map<String, Object>> esData = new HashMap<>();
            SearchRequest searchRequest = new SearchRequest("participants_structured.cmi.cmi-osteo");
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

            SearchResponse response = null;
            int i = 0;
            while (response == null || response.getHits().getHits().length != 0) {
                SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

                Calendar calendar = Calendar.getInstance();
                long now = calendar.getTimeInMillis();
                calendar.add(Calendar.MONTH, 6);
                long month6 = calendar.getTimeInMillis();
                sourceBuilder.query(QueryBuilders.rangeQuery("dsm.").gte(now).lte(month6));

                searchSourceBuilder.size(scrollSize);
                searchSourceBuilder.from(i * scrollSize);
                searchRequest.source(searchSourceBuilder);

                response = client.search(searchRequest, RequestOptions.DEFAULT);
                ElasticSearchUtil.addingParticipantStructuredHits(response, esData, "realm");
                i++;
            }
            Assert.assertNotEquals(0, esData.size());
        }
    }

    @Test
    public void createTestParticipantsInES() throws Exception {
        boolean addToDSMDB = false;

        try (RestHighLevelClient client = ElasticSearchUtil.getClientForElasticsearchCloud(cfg.getString("elasticSearch.url"), cfg.getString("elasticSearch.username"), cfg.getString("elasticSearch.password"))) {
            //getting a participant ES doc
            GetRequest getRequest = new GetRequest("participants_structured.cmi.angio", "_doc", "98JBYLZI33O0IFUMH9CS");
            GetResponse response = client.get(getRequest, RequestOptions.DEFAULT);
            Assert.assertNotNull(response);

            for (int i = 0; i < 100; i++) {
                String guid = "TEST000000000000000" + i;
                String hruid = "PT000" + i;
                //changing values to be able to create new participant
                Map<String, Object> source = response.getSource();
                Assert.assertNotNull(source);
                Object profile = source.get("profile");
                Assert.assertNotNull(profile);
                ((Map<String, Object>) profile).put("hruid", hruid);
                ((Map<String, Object>) profile).put("firstName", "Unit " + i);
                ((Map<String, Object>) profile).put("lastName", "Test " + i);
                ((Map<String, Object>) profile).put("guid", guid);
                Object medicalProviders = source.get("medicalProviders");
                List<Map<String, Object>> medicalProvidersList = ((List<Map<String, Object>>) medicalProviders);
                int counter = 0;
                for (Map<String, Object> medicalProviderMap : medicalProvidersList) {
                    medicalProviderMap.put("guid", "MP0" + counter + hruid);

                    //add participant and institution into DSM DB
                    if (addToDSMDB) { //only use if you want your dsm db to have the participants as well
                        TestHelper.addTestParticipant("Angio", guid, hruid, "MP0" + counter + hruid, "20191022", true);
                    }
                    counter++;
                }
                Assert.assertNotNull(medicalProviders);

                //adding new participant into ES
                IndexRequest indexRequest = new IndexRequest("participants_structured.cmi.angio", "_doc", guid).source(source);
                UpdateRequest updateRequest = new UpdateRequest("participants_structured.cmi.angio", "_doc", guid).doc(source).upsert(indexRequest);
                client.update(updateRequest, RequestOptions.DEFAULT);

                //getting a participant ES doc
                GetRequest getRequestAfter = new GetRequest("participants_structured.cmi.angio", "_doc", guid);
                GetResponse responseAfter = client.get(getRequestAfter, RequestOptions.DEFAULT);
                Assert.assertNotNull(responseAfter);

                //changing values to be able to create new participant
                Map<String, Object> sourceAfter = responseAfter.getSource();
                Assert.assertNotNull(sourceAfter);
                logger.info("added participant #" + i);
            }
        }
    }
}