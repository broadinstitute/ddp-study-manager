package org.broadinstitute.dsm;

import org.broadinstitute.dsm.db.dto.ddp.participant.ParticipantDataDto;
import org.broadinstitute.dsm.util.ParticipantUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

public class ParticipantTest {

    List<ParticipantDataDto> participantDatas;
    List<ParticipantDataDto> newParticipantDatas;

    @Before
    public void initializeParticipantDatas() {
        participantDatas = List.of(
                new ParticipantDataDto("testId", 19, "testFieldType",
                        "{\"DATSTAT_ALTPID\":\"testId\", \"COLLABORATOR_PARTICIPANT_ID\":\"id1\", \"DATSTAT_ALTEMAIL\":\"email\"}",
                        0, null),
                new ParticipantDataDto("testId2", 19, "testFieldType",
                        "{\"COLLABORATOR_PARTICIPANT_ID\":\"id2\", \"DATSTAT_ALTEMAIL\":\"email\"}",
                        0, null),
                new ParticipantDataDto("testId3", 19, "testFieldType",
                        "{\"COLLABORATOR_PARTICIPANT_ID\":\"id3\", \"DATSTAT_ALTEMAIL\":\"email1\"}",
                        0, null)
        );

        newParticipantDatas = List.of(
                new ParticipantDataDto("testId", 19, "testFieldType",
                        "{\"IS_APPLICANT\":\"true\", \"COLLABORATOR_PARTICIPANT_ID\":\"id1\", \"DATSTAT_ALTEMAIL\":\"email\"}",
                        0, null),
                new ParticipantDataDto("testId2", 19, "testFieldType",
                        "{\"COLLABORATOR_PARTICIPANT_ID\":\"id2\", \"DATSTAT_ALTEMAIL\":\"email\"}",
                        0, null),
                new ParticipantDataDto("testId3", 19, "testFieldType",
                        "{\"COLLABORATOR_PARTICIPANT_ID\":\"id3\", \"DATSTAT_ALTEMAIL\":\"email1\"}",
                        0, null)
        );
    }

    @Test
    public void checkApplicant() {
        String collaboratorParticipantId1 = "id1";

        Assert.assertTrue(ParticipantUtil.checkApplicantEmail(collaboratorParticipantId1, participantDatas));
    }

    @Test
    public void checkMemberWithSameEmail() {
        String collaboratorParticipantId2 = "id2";

        Assert.assertTrue(ParticipantUtil.checkApplicantEmail(collaboratorParticipantId2, participantDatas));
    }

    @Test
    public void checkMemberWithDifferentEmail() {
        String collaboratorParticipantId3 = "id3";

        Assert.assertFalse(ParticipantUtil.checkApplicantEmail(collaboratorParticipantId3, participantDatas));
    }

    @Test
    public void checkNewApplicant() {
        String collaboratorParticipantId1 = "id1";

        Assert.assertTrue(ParticipantUtil.checkApplicantEmail(collaboratorParticipantId1, newParticipantDatas));
    }

    @Test
    public void checkNewMemberWithSameEmail() {
        String collaboratorParticipantId2 = "id2";

        Assert.assertTrue(ParticipantUtil.checkApplicantEmail(collaboratorParticipantId2, newParticipantDatas));
    }

    @Test
    public void checkNewMemberWithDifferentEmail() {
        String collaboratorParticipantId3 = "id3";

        Assert.assertFalse(ParticipantUtil.checkApplicantEmail(collaboratorParticipantId3, newParticipantDatas));
    }

}
