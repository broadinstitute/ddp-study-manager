package org.broadinstitute.dsm.util;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.NonNull;
import org.broadinstitute.dsm.db.dto.participant.data.ParticipantDataDto;
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

    public static boolean checkProbandEmail(String collaboratorParticipantId, List<ParticipantDataDto> participantDatas) {
        String probandEmail = null, currentParticipantEmail = null;
        for (ParticipantDataDto participantData: participantDatas) {
            String data = participantData.getData();
            if (data == null) {
                continue;
            }
            JsonObject dataJsonObject = gson.fromJson(data, JsonObject.class);
            if (!dataJsonObject.has(FamilyMemberConstants.MEMBER_TYPE) || !dataJsonObject.has(FamilyMemberConstants.EMAIL)
                    || !dataJsonObject.has(FamilyMemberConstants.COLLABORATOR_PARTICIPANT_ID)) {
                return false;
            }
            for (Map.Entry<String, JsonElement> entry: dataJsonObject.entrySet()) {
                if (dataJsonObject.get(FamilyMemberConstants.MEMBER_TYPE).getAsString().equals(FamilyMemberConstants.MEMBER_TYPE_SELF)) {
                    currentParticipantEmail = dataJsonObject.get(FamilyMemberConstants.EMAIL).getAsString();
                }
                if (collaboratorParticipantId.equals(dataJsonObject.get(FamilyMemberConstants.COLLABORATOR_PARTICIPANT_ID).getAsString())) {
                    probandEmail = dataJsonObject.get(FamilyMemberConstants.EMAIL).getAsString();
                }
            }
        }
        return probandEmail != null && probandEmail.equals(currentParticipantEmail);
    }
}
