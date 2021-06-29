package org.broadinstitute.dsm.util;

import com.google.gson.Gson;
import lombok.NonNull;
import org.broadinstitute.dsm.db.dto.ddp.participant.ParticipantDataDto;
import org.broadinstitute.dsm.model.participant.data.FamilyMemberConstants;

import java.util.List;
import java.util.Map;

public class ParticipantUtil {

    private static final Gson gson = new Gson();

    public static final String DDP_PARTICIPANT_ID = "ddpParticipantId";

    public static boolean isHruid(@NonNull String participantId) {
        final String hruidCheck = "^P\\w{5}$";
        return participantId.matches(hruidCheck);
    }

    public static boolean isGuid(@NonNull String participantId) {
        return participantId.length() == 20;
    }

    public static boolean checkProbandEmail(String collaboratorParticipantId, List<ParticipantDataDto> participantDatas) {
        String probandEmail = null, currentParticipantEmail = null;
        for (ParticipantDataDto participantData: participantDatas) {
            String data = participantData.getData();
            if (data == null) {
                continue;
            }
            Map<String, String> dataMap = gson.fromJson(data, Map.class);
            if (!dataMap.containsKey(FamilyMemberConstants.MEMBER_TYPE)
                    || !dataMap.containsKey(FamilyMemberConstants.COLLABORATOR_PARTICIPANT_ID)) {
                return false;
            }
            if (dataMap.get(FamilyMemberConstants.MEMBER_TYPE).equals(FamilyMemberConstants.MEMBER_TYPE_SELF) &&
                    collaboratorParticipantId.equals(dataMap.get(FamilyMemberConstants.COLLABORATOR_PARTICIPANT_ID))) {
                return true;
            } else {
                if (!dataMap.containsKey(FamilyMemberConstants.EMAIL)) {
                    continue;
                }
                if (dataMap.get(FamilyMemberConstants.MEMBER_TYPE).equals(FamilyMemberConstants.MEMBER_TYPE_SELF)) {
                    probandEmail = dataMap.get(FamilyMemberConstants.EMAIL);
                }
                if (collaboratorParticipantId.equals(dataMap.get(FamilyMemberConstants.COLLABORATOR_PARTICIPANT_ID))) {
                    currentParticipantEmail = dataMap.get(FamilyMemberConstants.EMAIL);
                }
            }
        }
        return probandEmail != null && probandEmail.equals(currentParticipantEmail);
    }
}
