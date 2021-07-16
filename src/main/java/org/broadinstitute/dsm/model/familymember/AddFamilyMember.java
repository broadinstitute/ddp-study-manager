package org.broadinstitute.dsm.model.familymember;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.dsm.db.UserDto;
import org.broadinstitute.dsm.db.dao.ddp.instance.DDPInstanceDao;
import org.broadinstitute.dsm.db.dao.fieldsettings.FieldSettingsDao;
import org.broadinstitute.dsm.db.dao.user.UserDao;
import org.broadinstitute.dsm.db.dto.ddp.participant.ParticipantDataDto;
import org.broadinstitute.dsm.model.fieldsettings.FieldSettings;
import org.broadinstitute.dsm.model.participant.data.AddFamilyMemberPayload;
import org.broadinstitute.dsm.model.participant.data.FamilyMemberConstants;
import org.broadinstitute.dsm.model.participant.data.FamilyMemberDetails;
import org.broadinstitute.dsm.model.participant.data.ParticipantData;
import org.broadinstitute.dsm.util.ParticipantUtil;

public abstract class AddFamilyMember {

    protected AddFamilyMemberPayload addFamilyMemberPayload;
    protected ParticipantData participantData;
    protected int ddpInstanceId;
    protected DDPInstanceDao ddpInstanceDao;
    protected String studyGuid;
    protected String participantId;

    public AddFamilyMember(AddFamilyMemberPayload addFamilyMemberPayload) {
        this.addFamilyMemberPayload = Objects.requireNonNull(addFamilyMemberPayload);
        this.participantData = new ParticipantData();
        ddpInstanceDao = new DDPInstanceDao();
        studyGuid = addFamilyMemberPayload.getRealm().orElseThrow();
        participantId = addFamilyMemberPayload.getParticipantId().orElseThrow();
        ddpInstanceId = ddpInstanceDao.getDDPInstanceIdByGuid(studyGuid);
    }

    public long addFamilyMember() {
        prepareFamilyMemberData();
        copyProbandData();
        addDefaultOptionsValueToData();
        long createdParticipantDataId = this.participantData.insertParticipantData(
                new UserDao().get(addFamilyMemberPayload.getUserId().orElse(0)).flatMap(UserDto::getEmail).orElse("SYSTEM"));
        exportDataToEs();
        return createdParticipantDataId;
    }

    protected void prepareFamilyMemberData() {
        FamilyMemberDetails familyMemberDetails = addFamilyMemberPayload.getData().orElseThrow();
        String fieldTypeId =  studyGuid + ParticipantData.FIELD_TYPE;
        participantData.setDdpParticipantId(participantId);
        participantData.setDdpInstanceId(ddpInstanceId);
        participantData.setFieldTypeId(fieldTypeId);
        familyMemberDetails.setFamilyId(addFamilyMemberPayload.getOrGenerateFamilyId());
        familyMemberDetails.setCollaboratorParticipantId(addFamilyMemberPayload.generateCollaboratorParticipantId());
        if (FamilyMemberConstants.MEMBER_TYPE_SELF.equalsIgnoreCase(familyMemberDetails.getMemberType()))
            familyMemberDetails.setEmail(ParticipantUtil.getParticipantEmailById(
                    ddpInstanceDao.getEsParticipantIndexByStudyGuid(studyGuid).orElse(""),
                    addFamilyMemberPayload.getParticipantId().orElse("")));
        this.participantData.setData(familyMemberDetails.toMap());
    }

    protected void copyProbandData() {
        boolean isCopyProband = addFamilyMemberPayload.getCopyProbandInfo().orElse(Boolean.FALSE);
        if (!isCopyProband || StringUtils.isBlank(addFamilyMemberPayload.getParticipantId().orElse(""))) return;
        Map<String, String> participantDataData = participantData.getData();
        if (Objects.isNull(participantDataData)) throw new NoSuchElementException();
        List<ParticipantDataDto> participantDataByParticipantId =
                participantData.getParticipantDataByParticipantId(addFamilyMemberPayload.getParticipantId().orElse(""));
        Optional<ParticipantDataDto> maybeProbandData = participantData.findProband(participantDataByParticipantId);
        Optional<ParticipantData> maybeParticipantData = maybeProbandData.map(ParticipantData::parseDto);
        maybeParticipantData.ifPresent(participantData -> participantData.getData().forEach(participantDataData::putIfAbsent));
    }

    public void addDefaultOptionsValueToData() {
        participantData.addDefaultOptionsValueToData(getDefaultOptions());
    }

    public abstract void exportDataToEs();

    private Map<String, String> getDefaultOptions() {
        FieldSettingsDao fieldSettingsDao = FieldSettingsDao.of();
        FieldSettings fieldSettings = new FieldSettings();
        return fieldSettings.getColumnsWithDefaultOptions(fieldSettingsDao.getFieldSettingsByOptionAndInstanceId(ddpInstanceId));
    }


}
