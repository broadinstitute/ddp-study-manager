package org.broadinstitute.dsm.model.participant.data;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

import com.google.gson.Gson;
import org.broadinstitute.dsm.db.dto.ddp.participant.ParticipantDataDto;
import org.junit.Assert;
import org.junit.Test;

public class ParticipantDataTest {

    private static final String[] MEMBER_TYPES = {"SELF", "SISTER", "SON", "MOTHER", "FATHER", "COUSIN"};
    private static final Gson GSON = new Gson();



    @Test
    public void testFindProband() {
        ParticipantData participantData = new ParticipantData();
        List<ParticipantDataDto> participantDataDtoList = generateParticipantData();
        Optional<ParticipantDataDto> maybeProbandData = participantData.findProband(participantDataDtoList);
        Map<String, String> map = GSON.fromJson(maybeProbandData.map(ParticipantDataDto::getData).orElse(""), Map.class);
        Assert.assertEquals("SELF", map.get("MEMBER_TYPE"));
    }

    private List<ParticipantDataDto> generateParticipantData() {
        Random random = new Random();
        List<ParticipantDataDto> participantDataDtoList = new ArrayList<>();
        for (int i = 0; i < MEMBER_TYPES.length; i++) {
            String memberType = MEMBER_TYPES[i];
            int randomGeneratedFamilyId = random.nextInt();
            String familyId = random.nextInt(1000) + 1 + "_" + ("SELF".equals(memberType) ? 3 : randomGeneratedFamilyId == 3 ? randomGeneratedFamilyId + 1 : randomGeneratedFamilyId);
            String collaboratorParticipantId = "STUDY" + "_" + familyId;
            String email = "SELF".equals(memberType) ? "self@mail.com" : MEMBER_TYPES[1 + random.nextInt(MEMBER_TYPES.length-1)] + "@mail.com";
            FamilyMemberDetails familyMemberDetails = new FamilyMemberDetails(
                    "John" + i,
                    "Doe" + i,
                    memberType,
                    familyId,
                    collaboratorParticipantId);
            familyMemberDetails.setEmail(email);
            String data = GSON.toJson(familyMemberDetails);
            ParticipantDataDto participantDataDto =
                    new ParticipantDataDto(collaboratorParticipantId, i, "", data, System.currentTimeMillis(), "SYSTEM");
            participantDataDtoList.add(participantDataDto);
        }
        return participantDataDtoList;
    }


}