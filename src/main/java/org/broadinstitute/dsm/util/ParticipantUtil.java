package org.broadinstitute.dsm.util;

import com.google.gson.Gson;
import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.dsm.db.dao.ddp.instance.DDPInstanceDao;
import org.broadinstitute.dsm.db.dto.ddp.participant.ParticipantDataDto;
import org.broadinstitute.dsm.model.elasticsearch.ESProfile;
import org.broadinstitute.dsm.model.elasticsearch.ElasticSearch;
import org.broadinstitute.dsm.model.participant.data.FamilyMemberConstants;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ParticipantUtil {

    private static final Gson gson = new Gson();

    public static final String DDP_PARTICIPANT_ID = "ddpParticipantId";
    public static final String TRUE = "true";

    public static boolean isHruid(@NonNull String participantId) {
        final String hruidCheck = "^P\\w{5}$";
        return participantId.matches(hruidCheck);
    }

    public static boolean isGuid(@NonNull String participantId) {
        return participantId.length() == 20;
    }

    public static boolean checkApplicantEmail(String collaboratorParticipantId, List<ParticipantDataDto> participantDatas) {
        String applicantEmail = null, currentParticipantEmail = null;
        for (ParticipantDataDto participantData: participantDatas) {
            String data = participantData.getData().orElse(null);
            if (data == null) {
                continue;
            }
            Map<String, String> dataMap = gson.fromJson(data, Map.class);
            if (!dataMap.containsKey(FamilyMemberConstants.COLLABORATOR_PARTICIPANT_ID)) {
                return false;
            }

            boolean isOldApplicant = dataMap.containsKey(FamilyMemberConstants.DATSTAT_ALTPID)
                    && dataMap.get(FamilyMemberConstants.DATSTAT_ALTPID).equals(participantData.getDdpParticipantId().orElse(""));
            boolean isNewApplicant = dataMap.containsKey(FamilyMemberConstants.IS_APPLICANT)
                    && TRUE.equals(dataMap.get(FamilyMemberConstants.IS_APPLICANT));
            boolean isCurrentParticipant = collaboratorParticipantId.equals(dataMap.get(FamilyMemberConstants.COLLABORATOR_PARTICIPANT_ID));

            if ((isNewApplicant || isOldApplicant) && isCurrentParticipant) {
                return true;
            } else {
                if (isNewApplicant || isOldApplicant) {
                    applicantEmail = dataMap.get(FamilyMemberConstants.EMAIL);
                }
                if (isCurrentParticipant) {
                    currentParticipantEmail = dataMap.get(FamilyMemberConstants.EMAIL);
                }
            }
        }
        return applicantEmail != null && applicantEmail.equals(currentParticipantEmail);
    }

    public static String getParticipantEmailById(String esParticipantIndex, String pId) {
        if (StringUtils.isBlank(esParticipantIndex) || StringUtils.isBlank(pId)) throw new IllegalArgumentException();
        StringBuilder email = new StringBuilder();
        ElasticSearch participantESDataByParticipantId =
                ElasticSearchUtil.getParticipantESDataByParticipantId(esParticipantIndex, pId)
                .orElse(new ElasticSearch.Builder().build());
        email.append(participantESDataByParticipantId.getProfile()
                .map(ESProfile::getEmail)
                .orElse(""));
        return email.toString();
    }
}
