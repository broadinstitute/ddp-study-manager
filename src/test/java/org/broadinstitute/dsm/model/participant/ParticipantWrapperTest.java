package org.broadinstitute.dsm.model.participant;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.broadinstitute.dsm.model.elasticsearch.ESProfile;
import org.broadinstitute.dsm.model.elasticsearch.ElasticSearch;
import org.broadinstitute.dsm.model.elasticsearch.ElasticSearchParticipantDto;
import org.broadinstitute.dsm.model.elasticsearch.ElasticSearchable;
import org.junit.Assert;
import org.junit.Test;

public class ParticipantWrapperTest {
    public static final int PROXIES_QUANTITY = 5;

//    private static ParticipantWrapper participantWrapper;
//
//    @BeforeClass
//    public static void setUp() {
//        new ParticipantWrapperPayload.Builder()
//                .withDdpInstanceDto(DDPInstanceDto.of(false, false, false))
//                .
//        participantWrapper = new ParticipantWrapper()
//    }


    @Test
    public void getParticipantIdFromElasticList() {
        ParticipantWrapperPayload participantWrapperPayload = new ParticipantWrapperPayload.Builder()
                .build();
        ParticipantWrapper participantWrapper = new ParticipantWrapper(participantWrapperPayload, new ElasticSearchTest());
        List<ElasticSearchParticipantDto> elasticSearchList = participantWrapper.getElasticSearchable().getParticipantsWithinRange("", 0, 50).getEsParticipants();
        List<String> participantIds = participantWrapper.getParticipantIdsFromElasticList(elasticSearchList);
        Assert.assertEquals(10, participantIds.size());
    }

    @Test
    public void getProxiesFromElasticList() {
        ParticipantWrapperPayload participantWrapperPayload = new ParticipantWrapperPayload.Builder()
                .build();
        ParticipantWrapper participantWrapper = new ParticipantWrapper(participantWrapperPayload, new ElasticSearchTest());
        List<ElasticSearchParticipantDto> elasticSearchList = participantWrapper.getElasticSearchable().getParticipantsWithinRange("", 0, 50).getEsParticipants();
        Map<String, List<String>> proxyIds = participantWrapper.getProxiesIdsFromElasticList(elasticSearchList);
        Assert.assertTrue(proxyIds.size() > 0);
        Assert.assertEquals(PROXIES_QUANTITY, proxyIds.values().stream().findFirst().get().size());
    }

    @Test
    public void getProxiesWithParticipantIdsByProxiesIds() {
        ParticipantWrapperPayload participantWrapperPayload = new ParticipantWrapperPayload.Builder()
                .build();
        ElasticSearchTest elasticSearchable = new ElasticSearchTest();
        ParticipantWrapper participantWrapper = new ParticipantWrapper(participantWrapperPayload, elasticSearchable);
        List<ElasticSearchParticipantDto> elasticSearchList = participantWrapper.getElasticSearchable().getParticipantsWithinRange("", 0, 50).getEsParticipants();
        Map<String, List<String>> proxiesIdsFromElasticList = participantWrapper.getProxiesIdsFromElasticList(elasticSearchList);
        Map<String, List<ElasticSearchParticipantDto>> proxiesByParticipantIds = participantWrapper.getProxiesWithParticipantIdsByProxiesIds(
                "", proxiesIdsFromElasticList);
        Assert.assertEquals(proxiesByParticipantIds.keySet().size(), proxiesByParticipantIds.keySet().size());
        String parentId = proxiesIdsFromElasticList.keySet().stream().findFirst().get();
        String proxyId = proxiesIdsFromElasticList.values().stream().findFirst().get().get(0);
        Assert.assertEquals(proxiesByParticipantIds.get(parentId).get(0).getParticipantId(), proxyId);
    }

    private static class ElasticSearchTest implements ElasticSearchable {

        ElasticSearch elasticSearch = new ElasticSearch();

        @Override
        public ElasticSearch getParticipantsWithinRange(String esParticipantsIndex, int from, int to) {
            List<ElasticSearchParticipantDto> result = Stream.generate(() -> {
                        ESProfile esProfile = new ESProfile();
                        esProfile.setParticipantGuid(randomGuidGenerator());
                        return new ElasticSearchParticipantDto.Builder()
                                .withProfile(esProfile)
                                .withProxies(generateProxies())
                                .build();
                    })
                    .limit(10)
                    .collect(Collectors.toList());
            elasticSearch.setEsParticipants(result);
            return elasticSearch;
        }

        @Override
        public ElasticSearch getParticipantsByIds(String esParticipantIndex, List<String> participantIds) {
            List<ElasticSearchParticipantDto> result = new ArrayList<>();
            participantIds.forEach(pId -> {
                ESProfile esProfile = new ESProfile();
                esProfile.setParticipantGuid(pId);
                result.add(
                        new ElasticSearchParticipantDto.Builder()
                                .withProfile(esProfile)
                                .build()
                );
            });
            elasticSearch.setEsParticipants(result);
            return elasticSearch;
        }

        @Override
        public long getParticipantsSize(String esParticipantsIndex) {
            return 0;
        }

        @Override
        public ElasticSearch getParticipantsByRangeAndFilter(String esParticipantsIndex, int from, int to, String filter) {
            return null;
        }

        @Override
        public ElasticSearch getParticipantsByRangeAndIds(String participantIndexES, int from, int to, List<String> participantIds) {
            return null;
        }
    }


    @Test
    public void testGuidGenerator() {
        String guid = randomGuidGenerator();
        Assert.assertEquals(20, guid.length());
    }

    public static List<String> generateProxies() {
        return Stream
                .generate(ParticipantWrapperTest::randomGuidGenerator)
                .limit(PROXIES_QUANTITY)
                .collect(Collectors.toList());
    }

    public static String randomGuidGenerator() {
        char[] letters = new char[] {'A', 'B', 'C', 'D', 'F', 'G', 'H', 'I', 'G'};
        return generateParticipantId(letters, 20);
    }

    public static String randomLegacyAltPidGenerator() {
        char[] letters = new char[] {'a', 'B', 'C', 'd', 'F', 'g', 'H', 'I', 'j', 'K', 'L', 'm', 'X', 'Y', 'z'};
        return generateParticipantId(letters, 50);
    }

    private static String generateParticipantId(char[] letters, int stringSize) {
        StringBuilder guid = new StringBuilder();
        Random rand = new Random();
        for (int i = 1; i <= stringSize; i++) {
            if (i % 5 == 0) {
                guid.append(rand.nextInt(10));
            } else {
                guid.append(letters[rand.nextInt(letters.length)]);
            }
        }
        return guid.toString();
    }
}