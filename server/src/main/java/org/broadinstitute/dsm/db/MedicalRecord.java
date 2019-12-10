package org.broadinstitute.dsm.db;

import com.google.gson.Gson;
import lombok.Data;
import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.ddp.db.SimpleResult;
import org.broadinstitute.ddp.handlers.util.InstitutionDetail;
import org.broadinstitute.ddp.handlers.util.MedicalInfo;
import org.broadinstitute.dsm.DSMServer;
import org.broadinstitute.dsm.db.structure.ColumnName;
import org.broadinstitute.dsm.db.structure.TableName;
import org.broadinstitute.dsm.model.mbc.MBCInstitution;
import org.broadinstitute.dsm.model.mbc.MBCParticipant;
import org.broadinstitute.dsm.statics.*;
import org.broadinstitute.dsm.util.DBUtil;
import org.broadinstitute.dsm.util.DDPRequestUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.*;
import java.util.*;

import static org.broadinstitute.ddp.db.TransactionWrapper.inTransaction;

@Data
@TableName (
        name = DBConstants.DDP_MEDICAL_RECORD,
        alias = DBConstants.DDP_MEDICAL_RECORD_ALIAS,
        primaryKey = DBConstants.MEDICAL_RECORD_ID,
        columnPrefix = "")
public class MedicalRecord {

    private static final Logger logger = LoggerFactory.getLogger(MedicalRecord.class);

    public static final String SQL_SELECT_MEDICAL_RECORD = "SELECT p.ddp_participant_id, p.ddp_instance_id, " +
            "inst.institution_id, inst.ddp_institution_id, inst.type, inst.participant_id, " +
            "m.medical_record_id, m.name, m.contact, m.phone, m.fax, m.fax_sent, m.fax_sent_by, m.fax_confirmed, m.fax_sent_2, m.fax_sent_2_by, " +
            "m.fax_confirmed_2, m.fax_sent_3, m.fax_sent_3_by, m.fax_confirmed_3, m.mr_received, m.follow_ups, m.mr_document, m.mr_document_file_names, " +
            "m.mr_problem, m.mr_problem_text, m.unable_obtain, m.unable_obtain_text, m.duplicate, m.followup_required, m.followup_required_text, m.international, m.cr_required, m.pathology_present, " +
            "m.notes, m.additional_values_json, (SELECT sum(log.comments is null and log.type = \"DATA_REVIEW\") as reviewMedicalRecord FROM ddp_medical_record rec2 " +
            "LEFT JOIN ddp_medical_record_log log on (rec2.medical_record_id = log.medical_record_id) WHERE rec2.institution_id = inst.institution_id) as reviewMedicalRecord " +
            "FROM ddp_institution inst LEFT JOIN ddp_participant as p on (p.participant_id = inst.participant_id) " +
            "LEFT JOIN ddp_instance as ddp on (ddp.ddp_instance_id = p.ddp_instance_id) LEFT JOIN ddp_medical_record as m on (m.institution_id = inst.institution_id) " +
            "WHERE ddp.instance_name = ? AND inst.type != 'NOT_SPECIFIED' ";
    public static final String SQL_SELECT_MEDICAL_RECORD_LAST_CHANGED = "SELECT m.last_changed FROM ddp_institution inst " +
            "LEFT JOIN ddp_participant as p on (p.participant_id = inst.participant_id) LEFT JOIN ddp_instance as ddp on (ddp.ddp_instance_id = p.ddp_instance_id) " +
            "LEFT JOIN ddp_medical_record as m on (m.institution_id = inst.institution_id) WHERE p.participant_id = ?";
    public static final String SQL_ORDER_BY = " ORDER BY p.ddp_participant_id, inst.ddp_institution_id ASC";

    private final String medicalRecordId;
    private String institutionId;
    private String ddpInstitutionId;
    private String type;

    @ColumnName (DBConstants.NAME)
    private String name;

    @ColumnName (DBConstants.CONTACT)
    private String contact;

    @ColumnName (DBConstants.PHONE)
    private String phone;

    @ColumnName (DBConstants.FAX)
    private String fax;

    @ColumnName (DBConstants.FAX_SENT)
    private String faxSent;

    @ColumnName (DBConstants.FAX_SENT_BY)
    private String faxSentBy;

    @ColumnName (DBConstants.FAX_CONFIRMED)
    private String faxConfirmed;

    @ColumnName (DBConstants.FAX_SENT_2)
    private String faxSent2;

    @ColumnName (DBConstants.FAX_SENT_2_BY)
    private String faxSent2By;

    @ColumnName (DBConstants.FAX_CONFIRMED_2)
    private String faxConfirmed2;

    @ColumnName (DBConstants.FAX_SENT_3)
    private String faxSent3;

    @ColumnName (DBConstants.FAX_SENT_3_BY)
    private String faxSent3By;

    @ColumnName (DBConstants.FAX_CONFIRMED_3)
    private String faxConfirmed3;

    @ColumnName (DBConstants.MR_RECEIVED)
    private String mrReceived;

    @ColumnName (DBConstants.MR_DOCUMENT)
    private String mrDocument;

    @ColumnName (DBConstants.MR_DOCUMENT_FILE_NAMES)
    private String mrDocumentFileNames;

    @ColumnName (DBConstants.MR_PROBLEM)
    private boolean mrProblem;

    @ColumnName (DBConstants.MR_PROBLEM_TEXT)
    private String mrProblemText;

    @ColumnName (DBConstants.MR_UNABLE_OBTAIN)
    private boolean unableObtain;

    @ColumnName (DBConstants.MR_UNABLE_OBTAIN_TEXT)
    private String mrUnableToObtainText;

    @ColumnName (DBConstants.FOLLOWUP_REQUIRED)
    private boolean followUpRequired;

    @ColumnName (DBConstants.FOLLOWUP_REQUIRED_TEXT)
    private String followUpRequiredText;

    @ColumnName (DBConstants.DUPLICATE)
    private boolean duplicate;

    @ColumnName (DBConstants.INTERNATIONAL)
    private boolean international;

    @ColumnName (DBConstants.CR_REQUIRED)
    private boolean crRequired;

    @ColumnName (DBConstants.NOTES)
    private String mrNotes;

    @ColumnName (DBConstants.FOLLOW_UP_REQUESTS)
    private FollowUp[] followUps;

    @ColumnName (DBConstants.ADDITIONAL_VALUES)
    private String additionalValues;

    private boolean reviewMedicalRecord;

    @ColumnName (DBConstants.PATHOLOGY_PRESENT)
    private String pathologyPresent;

    public MedicalRecord(String medicalRecordId, String institutionId, String ddpInstitutionId) {
        this.medicalRecordId = medicalRecordId;
        this.institutionId = institutionId;
        this.ddpInstitutionId = ddpInstitutionId;
    }

    public MedicalRecord(String medicalRecordId, String institutionId, String ddpInstitutionId, String type,
                         String name, String contact, String phone, String fax,
                         String faxSent, String faxSentBy, String faxConfirmed,
                         String faxSent2, String faxSent2By, String faxConfirmed2,
                         String faxSent3, String faxSent3By, String faxConfirmed3,
                         String mrReceived, String mrDocument, String mrDocumentFileNames, boolean mrProblem,
                         String mrProblemText, boolean unableObtain, boolean duplicate, boolean international, boolean crRequired,
                         String pathologyPresent, String mrNotes, boolean reviewMedicalRecord,
                         FollowUp[] followUps, boolean followUpRequired, String followUpRequiredText, String additionalValues,
                         String mrUnableToObtainText) {
        this.medicalRecordId = medicalRecordId;
        this.institutionId = institutionId;
        this.ddpInstitutionId = ddpInstitutionId;
        this.type = type;
        this.name = name;
        this.contact = contact;
        this.phone = phone;
        this.fax = fax;
        this.faxSent = faxSent;
        this.faxSentBy = faxSentBy;
        this.faxConfirmed = faxConfirmed;
        this.faxSent2 = faxSent2;
        this.faxSent2By = faxSent2By;
        this.faxConfirmed2 = faxConfirmed2;
        this.faxSent3 = faxSent3;
        this.faxSent3By = faxSent3By;
        this.faxConfirmed3 = faxConfirmed3;
        this.mrReceived = mrReceived;
        this.mrDocument = mrDocument;
        this.mrDocumentFileNames = mrDocumentFileNames;
        this.mrProblem = mrProblem;
        this.mrProblemText = mrProblemText;
        this.unableObtain = unableObtain;
        this.duplicate = duplicate;
        this.international = international;
        this.crRequired = crRequired;
        this.pathologyPresent = pathologyPresent;
        this.mrNotes = mrNotes;
        this.reviewMedicalRecord = reviewMedicalRecord;
        this.followUps = followUps;
        this.followUpRequired = followUpRequired;
        this.followUpRequiredText = followUpRequiredText;
        this.additionalValues = additionalValues;
        this.mrUnableToObtainText = mrUnableToObtainText;
    }

    public static MedicalRecord getMedicalRecord(@NonNull ResultSet rs) throws SQLException {
        MedicalRecord medicalRecord = new MedicalRecord(
                rs.getString(DBConstants.MEDICAL_RECORD_ID),
                rs.getString(DBConstants.INSTITUTION_ID),
                rs.getString(DBConstants.DDP_INSTITUTION_ID),
                rs.getString(DBConstants.TYPE),
                rs.getString(DBConstants.NAME),
                rs.getString(DBConstants.CONTACT),
                rs.getString(DBConstants.PHONE),
                rs.getString(DBConstants.FAX),
                rs.getString(DBConstants.FAX_SENT),
                rs.getString(DBConstants.FAX_SENT_BY),
                rs.getString(DBConstants.FAX_CONFIRMED),
                rs.getString(DBConstants.FAX_SENT_2),
                rs.getString(DBConstants.FAX_SENT_2_BY),
                rs.getString(DBConstants.FAX_CONFIRMED_2),
                rs.getString(DBConstants.FAX_SENT_3),
                rs.getString(DBConstants.FAX_SENT_3_BY),
                rs.getString(DBConstants.FAX_CONFIRMED_3),
                rs.getString(DBConstants.MR_RECEIVED),
                rs.getString(DBConstants.MR_DOCUMENT),
                rs.getString(DBConstants.MR_DOCUMENT_FILE_NAMES),
                rs.getBoolean(DBConstants.MR_PROBLEM),
                rs.getString(DBConstants.MR_PROBLEM_TEXT),
                rs.getBoolean(DBConstants.MR_UNABLE_OBTAIN),
                rs.getBoolean(DBConstants.DUPLICATE),
                rs.getBoolean(DBConstants.INTERNATIONAL),
                rs.getBoolean(DBConstants.CR_REQUIRED),
                rs.getString(DBConstants.PATHOLOGY_PRESENT),
                rs.getString(DBConstants.NOTES),
                rs.getBoolean(DBConstants.REVIEW_MEDICAL_RECORD),
                new Gson().fromJson(rs.getString(DBConstants.FOLLOW_UP_REQUESTS), FollowUp[].class),
                rs.getBoolean(DBConstants.FOLLOWUP_REQUIRED),
                rs.getString(DBConstants.FOLLOWUP_REQUIRED_TEXT),
                rs.getString(DBConstants.ADDITIONAL_VALUES),
                rs.getString(DBConstants.MR_UNABLE_OBTAIN_TEXT)
        );
        return medicalRecord;
    }

    public static Map<String, List<MedicalRecord>> getMedicalRecords(@NonNull String realm) {
        return getMedicalRecords(realm, null);
    }

    public static Map<String, List<MedicalRecord>> getMedicalRecords(@NonNull String realm, String queryAddition) {
        logger.info("Collection mr information");
        Map<String, List<MedicalRecord>> medicalRecords = new HashMap<>();
        SimpleResult results = inTransaction((conn) -> {
            SimpleResult dbVals = new SimpleResult();
            try (PreparedStatement stmt = conn.prepareStatement(DBUtil.getFinalQuery(SQL_SELECT_MEDICAL_RECORD, queryAddition) + SQL_ORDER_BY)) {
                stmt.setString(1, realm);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        String ddpParticipantId = rs.getString(DBConstants.DDP_PARTICIPANT_ID);
                        List<MedicalRecord> medicalRecordList = new ArrayList<>();
                        if (medicalRecords.containsKey(ddpParticipantId)) {
                            medicalRecordList = medicalRecords.get(ddpParticipantId);
                        }
                        else {
                            medicalRecords.put(ddpParticipantId, medicalRecordList);
                        }
                        medicalRecordList.add(getMedicalRecord(rs));
                    }
                }
            }
            catch (SQLException ex) {
                dbVals.resultException = ex;
            }
            return dbVals;
        });

        if (results.resultException != null) {
            throw new RuntimeException("Couldn't get list of medicalRecords ", results.resultException);
        }
        logger.info("Got " + medicalRecords.size() + " participants medicalRecords in DSM DB for " + realm);
        return medicalRecords;
    }

    public static MedicalRecord getMedicalRecord(@NonNull String realm, @NonNull String ddpParticipantId, @NonNull String medicalRecordId) {
        SimpleResult results = inTransaction((conn) -> {
            SimpleResult dbVals = new SimpleResult();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_SELECT_MEDICAL_RECORD + QueryExtension.BY_DDP_PARTICIPANT_ID + QueryExtension.BY_MEDICAL_RECORD_ID)) {
                stmt.setString(1, realm);
                stmt.setString(2, ddpParticipantId);
                stmt.setString(3, medicalRecordId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        dbVals.resultValue = getMedicalRecord(rs);
                    }
                }
            }
            catch (SQLException ex) {
                dbVals.resultException = ex;
            }
            return dbVals;
        });

        if (results.resultException != null) {
            throw new RuntimeException("Error getting medicalRecord " + medicalRecordId + " of participant " + ddpParticipantId, results.resultException);
        }

        return (MedicalRecord) results.resultValue;
    }

    public static MedicalInfo getDDPInstitutionInfo(@NonNull DDPInstance ddpInstance, @NonNull String ddpParticipantId) {
        String dsmRequest = ddpInstance.getBaseUrl() + RoutePath.DDP_INSTITUTION_PATH.replace(RequestParameter.PARTICIPANTID, ddpParticipantId);
        try {
            MedicalInfo medicalInfo = DDPRequestUtil.getResponseObject(MedicalInfo.class, dsmRequest, ddpInstance.getName(), ddpInstance.isHasAuth0Token());
            return medicalInfo;
        }
        catch (IOException e) {
            throw new RuntimeException("Couldn't get participants and institutions for ddpInstance " + ddpInstance.getName(), e);
        }
    }

    public static MedicalInfo getInstitutionInfoFromDB(@NonNull String realm, @NonNull String ddpParticipantId) {
        MedicalInfo medicalInfo = new MedicalInfo();
        Map<String, MBCInstitution> mbcInstitutions = DSMServer.getMbcInstitutions();
        Map<String, MBCParticipant> mbcParticipants = DSMServer.getMbcParticipants();
        MBCParticipant mbcParticipant = mbcParticipants.get(ddpParticipantId);
        if (mbcParticipant != null && mbcInstitutions != null && !mbcInstitutions.isEmpty()) {
            logger.info(mbcInstitutions.size() + " institutions in MBC cached list");
            ArrayList<InstitutionDetail> institutionDetails = new ArrayList<>();
            List<MedicalRecord> medicalRecords = getInstitutions(realm, ddpParticipantId);
            Integer tissueConsent = null;
            for (MedicalRecord medicalRecord : medicalRecords) {
                MBCInstitution mbcInstitution = mbcInstitutions.get(medicalRecord.getDdpInstitutionId());
                if (mbcInstitution != null) {
                    InstitutionDetail institutionDetail = new InstitutionDetail(mbcInstitution.getPhysicianId(),
                            mbcInstitution.getName(), mbcInstitution.getInstitution(), mbcInstitution.getCity(),
                            mbcInstitution.getState(), MBCInstitution.PHYSICIAN);
                    institutionDetails.add(institutionDetail);
                    if (tissueConsent == null || tissueConsent == 1) {
                        //TODO how to handle if it changed?
                        tissueConsent = (!mbcInstitution.isFromBloodRelease()) ? 1 : 0;
                    }
                }
            }

            String dob = "";
            String consentDOB = mbcParticipant.getDOBConsent();
            String bloodDOB = mbcParticipant.getDOBBlood();
            if (StringUtils.isNotBlank(consentDOB)) {
                dob = consentDOB;
            }
            else if (StringUtils.isNotBlank(bloodDOB)) {
                dob = bloodDOB;
            }
            medicalInfo.setDob(dob);
            String diagnosed = "0/0";
            String diagnosedYear = mbcParticipant.getDiagnosedYear();
            String diagnosedMonth = mbcParticipant.getDiagnosedMonth();
            if (StringUtils.isNotBlank(diagnosedYear)) {
                if (StringUtils.isNotBlank(diagnosedMonth)) {
                    diagnosed = diagnosedMonth + "/" + diagnosedYear;
                }
                else {
                    diagnosed = diagnosedYear;
                }
            }
            medicalInfo.setDateOfDiagnosis(diagnosed);
            medicalInfo.setInstitutions(institutionDetails);
            medicalInfo.setDrawBloodConsent(1);
            medicalInfo.setTissueSampleConsent(tissueConsent);
        }
        else {
            logger.info("No information of participant in cache " + ddpParticipantId);
        }
        return medicalInfo;
    }

    public static List<MedicalRecord> getInstitutions(@NonNull String realm, @NonNull String ddpParticipantId) {
        List<MedicalRecord> medicalRecords = new ArrayList<>();
        SimpleResult results = inTransaction((conn) -> {
            SimpleResult dbVals = new SimpleResult();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_SELECT_MEDICAL_RECORD + QueryExtension.BY_DDP_PARTICIPANT_ID)) {
                stmt.setString(1, realm);
                stmt.setString(2, ddpParticipantId);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        medicalRecords.add(new MedicalRecord(
                                rs.getString(DBConstants.MEDICAL_RECORD_ID),
                                rs.getString(DBConstants.INSTITUTION_ID),
                                rs.getString(DBConstants.DDP_INSTITUTION_ID)));
                    }
                }
            }
            catch (SQLException ex) {
                dbVals.resultException = ex;
            }
            return dbVals;
        });

        if (results.resultException != null) {
            throw new RuntimeException("Error getting medicalRecords of participant " + ddpParticipantId, results.resultException);
        }

        return medicalRecords;
    }
}
