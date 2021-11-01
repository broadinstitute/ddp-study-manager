package org.broadinstitute.dsm.route;

import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.dsm.db.DDPInstance;
import org.broadinstitute.dsm.db.OncHistoryDetail;
import org.broadinstitute.dsm.db.dao.kit.BSPKitDao;
import org.broadinstitute.dsm.db.dto.kit.BSPKitDto;
import org.broadinstitute.dsm.db.dto.kit.ClinicalKitDto;
import org.broadinstitute.dsm.model.BSPKit;
import org.broadinstitute.dsm.model.bsp.BSPKitInfo;
import org.broadinstitute.dsm.model.bsp.BSPKitStatus;
import org.broadinstitute.dsm.model.ddp.DDPActivityConstants;
import org.broadinstitute.dsm.model.elasticsearch.*;
import org.broadinstitute.dsm.statics.RequestParameter;
import org.broadinstitute.dsm.util.NotificationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;
import spark.Route;

import java.util.*;

public class ClinicalKitsRoute implements Route {
    private String FIRSTNAME = "firstName";
    private String LASTNAME = "lastName";
    private String DATE_OF_BIRtH = "dateOfBirth";

    private static final Logger logger = LoggerFactory.getLogger(ClinicalKitsRoute.class);

    private NotificationUtil notificationUtil;

    public ClinicalKitsRoute(@NonNull NotificationUtil notificationUtil) {
        this.notificationUtil = notificationUtil;
    }

    @Override
    public Object handle(Request request, Response response) {
        String kitLabel = request.params(RequestParameter.LABEL);
        if (StringUtils.isBlank(kitLabel)) {
            throw new RuntimeException("Please include a kit label as a path parameter");
        }
        BSPKit bspKit = new BSPKit();
        if (!bspKit.canReceiveKit(kitLabel)) {
            Optional<BSPKitStatus> result = bspKit.getKitStatus(kitLabel, notificationUtil);
            if(result.isEmpty()){
                response.status(404);
                return null;
            }
            return result.get();
        }
        return getClinicalKit(kitLabel);
    }

    private ClinicalKitDto getClinicalKit(String kitLabel) {
        logger.info("Checking label " + kitLabel);
        BSPKitDao bspKitDao = new BSPKitDao();
        // this method already sets the received time, check for exited and deactivation and special behaviour, and triggers DDP, we don't need a new
        BSPKit bspKit = new BSPKit();
        ClinicalKitDto clinicalKit = new ClinicalKitDto();
        Optional<BSPKitInfo> maybeKitInfo = bspKit.receiveBSPKit(kitLabel, notificationUtil);
        maybeKitInfo.ifPresent(kitInfo -> {
            logger.info("Creating clinical kit to return to GP " + kitLabel);
            clinicalKit.setCollaboratorParticipantId(kitInfo.getCollaboratorParticipantId());
            clinicalKit.setSampleId(kitInfo.getCollaboratorSampleId());
            clinicalKit.setMaterialType(kitInfo.getMaterialInfo());
            clinicalKit.setVesselType(kitInfo.getReceptacleName());
            clinicalKit.setSampleType(kitInfo.getKitType());
            clinicalKit.setMfBarcode(kitLabel);
            clinicalKit.setSampleCollection(kitInfo.getSampleCollectionBarcode());
            Optional<BSPKitDto> bspKitQueryResult = bspKitDao.getBSPKitQueryResult(kitLabel);
            bspKitQueryResult.orElseThrow(() -> {throw new RuntimeException("kit label was not found "+kitLabel);});
            BSPKitDto maybeBspKitQueryResult = bspKitQueryResult.get();
            DDPInstance ddpInstance = DDPInstance.getDDPInstance(maybeBspKitQueryResult.getInstanceName());
            String hruid = maybeBspKitQueryResult.getBspParticipantId();
            if (hruid.indexOf('_') != -1) {
                hruid = maybeBspKitQueryResult.getBspParticipantId().substring(maybeBspKitQueryResult.getBspParticipantId().lastIndexOf('_') + 1);
            }
            ElasticSearchParticipantDto participantByShortId =
                    new ElasticSearch().getParticipantByShortId(ddpInstance.getParticipantIndexES(), hruid);
            setNecessaryParticipantDataToClinicalKit(clinicalKit, participantByShortId, kitInfo.getRealm());
        });
        maybeKitInfo.orElseThrow();
        return clinicalKit;

    }

    private void setNecessaryParticipantDataToClinicalKit(ClinicalKitDto clinicalKit,
                                                          ElasticSearchParticipantDto participantByShortId, String instanceName) {
        try {
            clinicalKit.setDateOfBirth(Objects.requireNonNull(participantByShortId).getDsm().map(ESDsm::getDateOfBirth).orElse(""));
            String firstName = participantByShortId.getProfile().map(ESProfile::getFirstName).orElse("");
            String lastName = participantByShortId.getProfile().map(ESProfile::getLastName).orElse("");
            String gender = getParticipantGender(participantByShortId, instanceName);
            clinicalKit.setFirstName(firstName);
            clinicalKit.setLastName(lastName);
            clinicalKit.setGender(gender);
        } catch (Exception e) {
            throw new RuntimeException("Participant doesn't exist / is not valid for kit ");
        }
    }

    private String getParticipantGender(ElasticSearchParticipantDto participantByShortId, String realm) {
        // if gender is set on tissue page use that
        List<String> list = new ArrayList();
        list.add(participantByShortId.getParticipantId());
        Map<String, List<OncHistoryDetail>> oncHistoryDetails = OncHistoryDetail.getOncHistoryDetailsByParticipantIds(realm, list);
        if(!oncHistoryDetails.isEmpty()){
            Optional<OncHistoryDetail> oncHistoryWithGender = oncHistoryDetails.get(participantByShortId.getParticipantId()).stream().filter(o -> StringUtils.isNotBlank(o.getGender())).findFirst();
            if(oncHistoryWithGender.isPresent()){
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
