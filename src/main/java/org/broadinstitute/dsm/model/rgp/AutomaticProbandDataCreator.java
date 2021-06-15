package org.broadinstitute.dsm.model.rgp;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import lombok.NonNull;
import org.broadinstitute.dsm.db.DDPInstance;
import org.broadinstitute.dsm.db.ParticipantData;
import org.broadinstitute.dsm.db.dao.bookmark.BookmarkDao;
import org.broadinstitute.dsm.db.dao.fieldsettings.FieldSettingsDao;
import org.broadinstitute.dsm.db.dao.ddp.participant.ParticipantDataDao;
import org.broadinstitute.dsm.db.dto.bookmark.BookmarkDto;
import org.broadinstitute.dsm.db.dto.fieldsettings.FieldSettingsDto;
import org.broadinstitute.dsm.model.ddp.DDPActivityConstants;
import org.broadinstitute.dsm.model.fieldsettings.FieldSettings;
import org.broadinstitute.dsm.model.participant.data.FamilyMemberConstants;
import org.broadinstitute.dsm.model.participant.data.FamilyMemberDetails;
import org.broadinstitute.dsm.model.participant.data.NewParticipantData;
import org.broadinstitute.dsm.statics.ESObjectConstants;
import org.broadinstitute.dsm.util.ElasticSearchUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AutomaticProbandDataCreator {


    private static final Logger logger = LoggerFactory.getLogger(AutomaticProbandDataCreator.class);
    public static final String RGP_FAMILY_ID = "rgp_family_id";

    private boolean isParticipantDataUpdated = false;

    public Map<String, List<ParticipantData>> setDefaultProbandDataIfNotExists(Map<String, List<ParticipantData>> participantData,
                                                                                      Map<String, Map<String, Object>> participantESData,
                                                                                      @NonNull DDPInstance instance) {
        if (participantESData == null) {
            logger.warn("Could not create proband/self data, participant ES data is null");
            return participantData;
        }
        BookmarkDao bookmarkDao = new BookmarkDao();
        ParticipantDataDao participantDataDao = new ParticipantDataDao();
        List<FieldSettingsDto> fieldSettingsByOptionAndInstanceId =
                new FieldSettingsDao().getFieldSettingsByOptionAndInstanceId(Integer.parseInt(instance.getDdpInstanceId()));
        Map<String, String> columnsWithDefaultOptions =
                new FieldSettings().getColumnsWithDefaultOptions(fieldSettingsByOptionAndInstanceId);
        for (Map.Entry<String, Map<String, Object>> entry: participantESData.entrySet()) {
            String pId = entry.getKey();
            List<ParticipantData> participantDataList = participantData.get(entry.getKey());
            Map<String, Object> profile = (Map<String, Object>) entry.getValue().get(ElasticSearchUtil.PROFILE);
            if (profile == null) {
                logger.warn("Could not create proband/self data, participant with id: " + pId + " does not have profile in ES");
                continue;
            }
            if (participantDataList == null) {
                extractAndInsertProbandFromESData(instance, entry, participantDataDao, bookmarkDao, columnsWithDefaultOptions);
            }
        }
        return isParticipantDataUpdated ? ParticipantData.getParticipantData(instance.getName()) : participantData;
    }

    private void extractAndInsertProbandFromESData(DDPInstance instance, Map.Entry<String, Map<String, Object>> esData,
                                                          ParticipantDataDao participantDataDao,
                                                          BookmarkDao bookmarkDao,
                                                          Map<String, String> columnsWithDefaultOptions) {
        String participantId = esData.getKey();
        NewParticipantData newParticipantData = new NewParticipantData(participantDataDao);
        Optional<BookmarkDto> maybeFamilyIdOfBookmark = bookmarkDao.getBookmarkByInstance(RGP_FAMILY_ID);
        Map<String, String> probandDataMap = extractProbandDefaultDataFromParticipantProfile(esData.getValue(), maybeFamilyIdOfBookmark);
        newParticipantData.setData(
                participantId,
                Integer.parseInt(instance.getDdpInstanceId()),
                instance.getName().toUpperCase() + NewParticipantData.FIELD_TYPE,
                probandDataMap
                );
        newParticipantData.addDefaultOptionsValueToData(columnsWithDefaultOptions);
        newParticipantData.insertParticipantData("SYSTEM");
        maybeFamilyIdOfBookmark.ifPresent(familyIdBookmarkDto -> {
            insertFamilyIdToDsmES(instance.getParticipantIndexES(), participantId, familyIdBookmarkDto.getValue());
            familyIdBookmarkDto.setValue(familyIdBookmarkDto.getValue() + 1);
            bookmarkDao.updateBookmarkValueByBookmarkId(familyIdBookmarkDto.getBookmarkId(), familyIdBookmarkDto.getValue());
        });
        isParticipantDataUpdated = true;
        logger.info("Automatic proband data for participant with id: " + participantId + " has been created");
    }

    private Map<String, String> extractProbandDefaultDataFromParticipantProfile(@NonNull Map<String, Object> esData,
                                                                                       Optional<BookmarkDto> maybeBookmark) {
        Map<String, Object> profile = (Map<String, Object>) esData.get(ElasticSearchUtil.PROFILE);
        List<Map<String, Object>> activities = (List<Map<String, Object>>) esData.get(ElasticSearchUtil.ACTIVITIES);
        Optional<Map<String, Object>> maybeEnrollmentActivity = activities.stream()
                .filter(a -> DDPActivityConstants.ACTIVITY_ENROLLMENT.equals(a.get(ElasticSearchUtil.ACTIVITY_CODE)))
                .findFirst();
        StringBuilder mobilePhone = new StringBuilder();
        maybeEnrollmentActivity.ifPresent(a -> {
            List<Map<String, Object>> questionsAnswers = (List<Map<String, Object>>) a.get(ElasticSearchUtil.QUESTIONS_ANSWER);
            Optional<Map<String, Object>> maybePhoneQuestionAnswer = questionsAnswers.stream()
                    .filter(q -> DDPActivityConstants.ENROLLMENT_ACTIVITY_PHONE.equals(q.get(DDPActivityConstants.DDP_ACTIVITY_STABLE_ID)))
                    .findFirst();
            maybePhoneQuestionAnswer.ifPresent(ans -> mobilePhone.append(ans.get(DDPActivityConstants.ACTIVITY_QUESTION_ANSWER)));
        });
        String firstName = (String) profile.get(ElasticSearchUtil.FIRST_NAME_FIELD);
        String lastName = (String) profile.get(ElasticSearchUtil.LAST_NAME_FIELD);
        String familyId = maybeBookmark
                .map(bookmarkDto -> String.valueOf(bookmarkDto.getValue()))
                .orElse((String) profile.get(ElasticSearchUtil.HRUID));
        String collaboratorParticipantId = familyId + "_" + FamilyMemberConstants.PROBAND_RELATIONSHIP_ID;
        String memberType = FamilyMemberConstants.MEMBER_TYPE_SELF;
        String email = (String) profile.get(ElasticSearchUtil.EMAIL_FIELD);
        FamilyMemberDetails probandMemberDetails =
                new FamilyMemberDetails(firstName, lastName, memberType, familyId, collaboratorParticipantId);
        probandMemberDetails.setMobilePhone(mobilePhone.toString());
        probandMemberDetails.setEmail(email);
        return probandMemberDetails.toMap();
    }

    void insertFamilyIdToDsmES(@NonNull String esIndex, @NonNull String participantId, @NonNull long familyId) {
        try {
            Map<String, Object> esObjectMap = ElasticSearchUtil.getObjectsMap(esIndex, participantId, ESObjectConstants.DSM);
            Map<String, Object> esDsmObjectMap = (Map<String, Object>) esObjectMap.get(ESObjectConstants.DSM);
            esDsmObjectMap.put(ESObjectConstants.FAMILY_ID, familyId);
            ElasticSearchUtil.updateRequest(participantId, esIndex, esObjectMap);
        } catch (Exception e) {
            logger.error("Could not insert family id for participant: " + participantId, e);
        }
    }
}
