package org.broadinstitute.dsm.model.elasticsearch;


import org.broadinstitute.dsm.model.participant.ParticipantWrapperTest;
import org.junit.Assert;
import org.junit.Test;

public class ElasticSearchTest {



    @Test
    public void getParticipantIdFromProfile() {
        ESProfile profile = esProfileGeneratorWithGuid();
        ElasticSearch elasticSearch = new ElasticSearch.Builder()
                .withProfile(profile)
                .build();
        String participantId = elasticSearch.getParticipantIdFromProfile();
        Assert.assertEquals(profile.getParticipantGuid(), participantId);
    }

    @Test
    public void getParticipantIdFromProfileIfGuidEmpty() {
        ESProfile esProfileWithLegacyAltPid = esProfileGeneratorWithLegacyAltPid();
        ElasticSearch elasticSearch = new ElasticSearch.Builder()
                .withProfile(esProfileWithLegacyAltPid)
                .build();
        String participantId = elasticSearch.getParticipantIdFromProfile();
        Assert.assertEquals(esProfileWithLegacyAltPid.getParticipantLegacyAltPid(), participantId);
    }

    @Test
    public void getParticipantIdFromProfileIfEmpty() {
        ESProfile esProfile = new ESProfile();
        ElasticSearch elasticSearch = new ElasticSearch.Builder()
                .build();
        String participantId = elasticSearch.getParticipantIdFromProfile();
        Assert.assertEquals("", participantId);
    }

    private static ESProfile esProfileGeneratorWithGuid() {
        ESProfile esProfile = new ESProfile();
        esProfile.setParticipantGuid(ParticipantWrapperTest.randomGuidGenerator());
        return esProfile;
    }

    private static ESProfile esProfileGeneratorWithLegacyAltPid() {
        ESProfile esProfile = new ESProfile();
        esProfile.setParticipantLegacyAltPid(ParticipantWrapperTest.randomLegacyAltPidGenerator());
        return esProfile;
    }



}