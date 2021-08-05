package org.broadinstitute.dsm.model.participant;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.broadinstitute.dsm.model.elasticsearch.ESProfile;
import org.broadinstitute.dsm.model.elasticsearch.ElasticSearch;
import org.broadinstitute.dsm.model.elasticsearch.ElasticSearchable;
import org.junit.Assert;
import org.junit.Test;

public class ParticipantWrapperTest {

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
        List<ElasticSearch> elasticSearchList = participantWrapper.getElasticSearchable().getParticipantsWithinRange("", 0, 50);
        List<String> participantIds = participantWrapper.getParticipantIdsFromElasticList(elasticSearchList);
        Assert.assertEquals(10, participantIds.size());
    }

    @Test
    public void getProxiesFromElasticList() {
        ParticipantWrapperPayload participantWrapperPayload = new ParticipantWrapperPayload.Builder()
                .build();
        ParticipantWrapper participantWrapper = new ParticipantWrapper(participantWrapperPayload, new ElasticSearchTest());
        List<ElasticSearch> elasticSearchList = participantWrapper.getElasticSearchable().getParticipantsWithinRange("", 0, 50);
        Map<String, List<String>> proxyIds = participantWrapper.getProxiesIdsFromElasticList(elasticSearchList);
    }


    private static class ElasticSearchTest implements ElasticSearchable {

        @Override
        public List<ElasticSearch> getParticipantsWithinRange(String esParticipantsIndex, int from, int to) {
            return Stream.generate(() -> {
                ESProfile esProfile = new ESProfile();
                esProfile.setParticipantGuid(randomGuidGenerator());
                return new ElasticSearch.Builder()
                        .withProfile(esProfile)
                        .build();
                })
                    .limit(10)
                    .collect(Collectors.toList());
        }
    }


    @Test
    public void testGuidGenerator() {
        String guid = randomGuidGenerator();
        Assert.assertEquals(20, guid.length());
    }

    private static String randomGuidGenerator() {
        StringBuilder guid = new StringBuilder();
        Random rand = new Random();
        char[] letters = new char[] {'A', 'B', 'C', 'D', 'F', 'G', 'H', 'I', 'G'};
        for (int i = 1; i <= 20; i++) {
            if (i % 5 == 0) {
                guid.append(rand.nextInt(10));
            } else {
                guid.append(letters[rand.nextInt(letters.length)]);
            }
        }
        return guid.toString();
    }
}