package org.broadinstitute.dsm.db.dto.kit;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.dsm.db.DDPInstance;
import org.broadinstitute.dsm.db.KitRequestShipping;
import org.broadinstitute.dsm.db.OncHistoryDetail;
import org.broadinstitute.dsm.db.dao.kit.ClinicalKitDao;
import org.broadinstitute.dsm.model.ddp.DDPActivityConstants;
import org.broadinstitute.dsm.model.elasticsearch.*;
import org.broadinstitute.dsm.util.ElasticSearchUtil;

import java.util.*;

@Data
public class ClinicalKitDto {

    @SerializedName ("participant_id")
    String collaboratorParticipantId;

    @SerializedName ("sample_id")
    String sampleId;

    @SerializedName ("sample_collection")
    String sampleCollection;

    @SerializedName ("material_type")
    String materialType;

    @SerializedName ("vessel_type")
    String vesselType;

    @SerializedName ("first_name")
    String firstName;

    @SerializedName ("last_name")
    String lastName;

    @SerializedName ("date_of_birth")
    String dateOfBirth;

    @SerializedName ("sample_type")
    String sampleType;

    @SerializedName ("gender")
    String gender;

    @SerializedName ("accession_number")
    String accessionNumber;

    @SerializedName ("kit_label")
    String mfBarcode;

    String ddpParticipantId;
    Integer ddpInstanceId;

    private final String FFPE_SECTION_KIT_TYPE = "FFPE-SECTION";
    private final String FFPE_SCROLLS_KIT_TYPE = "FFPE-SCROLL";
    private final String USS = "uss";
    private final String SCROLLS = "scrolls";

    public ClinicalKitDto(String collaboratorParticipantId, String sampleId, String sampleCollection, String materialType, String vesselType,
                          String firstName, String lastName, String dateOfBirth, String sampleType, String gender, String accessionNumber, String mfBarcode) {
        this.collaboratorParticipantId = collaboratorParticipantId;
        this.sampleId = sampleId;
        this.sampleCollection = sampleCollection;
        this.materialType = materialType;
        this.vesselType = vesselType;
        this.firstName = firstName;
        this.lastName = lastName;
        this.dateOfBirth = dateOfBirth;
        this.sampleType = sampleType;
        this.gender = gender;
        this.accessionNumber = accessionNumber;
        this.mfBarcode = mfBarcode;
    }

    public ClinicalKitDto() {
    }

    public void setSampleType(String kitType) {
        switch (kitType.toLowerCase()) {
            case "saliva":
                this.sampleType = "Normal";
                break;
            case "blood":
                this.sampleType = "N/A";
                break;
            default: //tissue
                this.sampleType = "Tumor";
                break;
        }
    }

    public void setGender(String genderString) {
        switch (genderString.toLowerCase()) {
            case "male":
                this.gender = "M";
                break;
            case "female":
                this.gender = "F";
                break;
            default: //intersex or prefer_not_answer
                this.gender = "U";
                break;
        }
    }



    public Optional<ClinicalKitDto> getClinicalKitBasedONSmId(String smIdValue) {
        ClinicalKitDao clinicalKitDao = new ClinicalKitDao();
        Optional<ClinicalKitDto> maybeClinicalKit = clinicalKitDao.getClinicalKitFromSMId(smIdValue);
        maybeClinicalKit.orElseThrow();
        ClinicalKitDto clinicalKitDto = maybeClinicalKit.get();
        DDPInstance ddpInstance = DDPInstance.getDDPInstanceById(clinicalKitDto.ddpInstanceId);
        clinicalKitDto.setNecessaryParticipantDataToClinicalKit(clinicalKitDto.ddpParticipantId, ddpInstance);
        return Optional.ofNullable(clinicalKitDto);
    }

    public void setNecessaryParticipantDataToClinicalKit(String ddpParticipantId, DDPInstance ddpInstance) {
        ElasticSearchUtil.getParticipantESDataByParticipantId(ddpInstance.getParticipantIndexES(), ddpParticipantId).ifPresentOrElse(
                maybeParticipant -> {
                    String shortId = maybeParticipant.getProfile().map(ESProfile::getHruid).get();
                    ElasticSearchParticipantDto participantByShortId =
                            new ElasticSearch().getParticipantByShortId(ddpInstance.getParticipantIndexES(), shortId);
                    try {
                        this.setDateOfBirth(Objects.requireNonNull(participantByShortId).getDsm().map(ESDsm::getDateOfBirth).orElse(""));
                        String firstName = participantByShortId.getProfile().map(ESProfile::getFirstName).orElse("");
                        String lastName = participantByShortId.getProfile().map(ESProfile::getLastName).orElse("");
                        String gender = getParticipantGender(participantByShortId, ddpInstance.getName());
                        this.setFirstName(firstName);
                        this.setLastName(lastName);
                        this.setGender(gender);
                        String collaboratorParticipantId = KitRequestShipping.getCollaboratorParticipantId(ddpInstance.getBaseUrl(), ddpInstance.getDdpInstanceId(), ddpInstance.isMigratedDDP(),
                                ddpInstance.getCollaboratorIdPrefix(), ddpParticipantId, shortId, null);
                        this.setCollaboratorParticipantId(collaboratorParticipantId);
                    }
                    catch (Exception e) {
                        throw new RuntimeException("Participant doesn't exist / is not valid for kit ");
                    }

                }, () -> {
                    throw new RuntimeException("Participant ES Data is not found for " + ddpParticipantId);
                });
    }

    private String getParticipantGender(ElasticSearchParticipantDto participantByShortId, String realm) {
        // if gender is set on tissue page use that
        List<String> list = new ArrayList();
        list.add(participantByShortId.getParticipantId());
        Map<String, List<OncHistoryDetail>> oncHistoryDetails = OncHistoryDetail.getOncHistoryDetailsByParticipantIds(realm, list);
        if (!oncHistoryDetails.isEmpty()) {
            Optional<OncHistoryDetail> oncHistoryWithGender = oncHistoryDetails.get(participantByShortId.getParticipantId()).stream().filter(o -> StringUtils.isNotBlank(o.getGender())).findFirst();
            if (oncHistoryWithGender.isPresent()) {
                return oncHistoryWithGender.get().getGender();
            }
        }
        //if gender is not set on tissue page get answer from "ABOUT_YOU.ASSIGNED_SEX"
        return participantByShortId.getActivities()
                .map(this::getGenderFromActivities)
                .orElse("U");
    }

    private String getGenderFromActivities(List<ESActivities> activities) {
        Optional<ESActivities> maybeAboutYouActivity = activities.stream()
                .filter(activity -> DDPActivityConstants.ACTIVITY_ABOUT_YOU.equals(activity.getActivityCode()))
                .findFirst();
        return (String) maybeAboutYouActivity.map(aboutYou -> {
            List<Map<String, Object>> questionsAnswers = aboutYou.getQuestionsAnswers();
            Optional<Map<String, Object>> maybeGenderQuestionAnswer = questionsAnswers.stream()
                    .filter(q -> DDPActivityConstants.ABOUT_YOU_ACTIVITY_GENDER.equals(q.get(DDPActivityConstants.DDP_ACTIVITY_STABLE_ID)))
                    .findFirst();
            return maybeGenderQuestionAnswer
                    .map(answer -> answer.get(DDPActivityConstants.ACTIVITY_QUESTION_ANSWER))
                    .orElse("");
        }).orElse("");
    }
}
