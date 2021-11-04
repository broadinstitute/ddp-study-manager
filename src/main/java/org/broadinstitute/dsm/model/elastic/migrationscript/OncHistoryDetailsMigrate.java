package org.broadinstitute.dsm.model.elastic.migrationscript;

import java.util.HashMap;
import java.util.Map;

import static org.broadinstitute.dsm.model.elastic.export.parse.TypeParser.*;
import static org.broadinstitute.dsm.model.elastic.export.parse.TypeParser.TEXT_KEYWORD_MAPPING;

public class OncHistoryDetailsMigrate {

    private static final Map<String, Object> oncHistoryDetails1 = Map.of (
            "ddpParticipantId", TEXT_KEYWORD_MAPPING,
            "ddpInstanceId", TEXT_KEYWORD_MAPPING,
            "institutionId", TEXT_KEYWORD_MAPPING,
            "ddpInstitutionId", TEXT_KEYWORD_MAPPING,
            "type", TEXT_KEYWORD_MAPPING,
            "participantId", TEXT_KEYWORD_MAPPING,
            "medicalRecordId", TEXT_KEYWORD_MAPPING,
            "name", TEXT_KEYWORD_MAPPING,
            "contact", TEXT_KEYWORD_MAPPING );

    private static final Map<String, Object> oncHistoryDetails2 = Map.of (
            "phone", TEXT_KEYWORD_MAPPING,
            "fax", TEXT_KEYWORD_MAPPING,
            "faxSent", DATE_MAPPING, //DATE?
            "faxSentBy", TEXT_KEYWORD_MAPPING,
            "faxConfirmed", DATE_MAPPING, //DATE?
            "faxSent2", DATE_MAPPING, //DATE?
            "faxSent2By", TEXT_KEYWORD_MAPPING,
            "faxConfirmed2", DATE_MAPPING, //DATE?
            "faxSent3", DATE_MAPPING, //DATE?
            "faxSent3By", TEXT_KEYWORD_MAPPING);

    private static final Map<String, Object> oncHistoryDetails3 = Map.of (
            "faxConfirmed3", DATE_MAPPING, //DATE?
            "mrReceived", DATE_MAPPING, //DATE?
            "followUps", TEXT_KEYWORD_MAPPING,
            "mrDocument", TEXT_KEYWORD_MAPPING,
            "mrDocumentFileName", TEXT_KEYWORD_MAPPING,
            "mrProblem", BOOLEAN_MAPPING,
            "mrProblemText", TEXT_KEYWORD_MAPPING,
            "unableObtain", BOOLEAN_MAPPING,
            "unableObtainText", TEXT_KEYWORD_MAPPING,
            "duplicate", BOOLEAN_MAPPING);

    private static final Map<String, Object> oncHistoryDetails4 = Map.of (
            "followUpRequired", BOOLEAN_MAPPING,
            "followUpRequiredText", TEXT_KEYWORD_MAPPING,
            "international", BOOLEAN_MAPPING,
            "crRequired", BOOLEAN_MAPPING,
            "pathologyPresent", TEXT_KEYWORD_MAPPING,
            "notes", TEXT_KEYWORD_MAPPING,
            "additionalValuesJson", TEXT_KEYWORD_MAPPING,
            "reviewMedicalRecord", TEXT_KEYWORD_MAPPING
    );

    private static final Map<String, Object> oncHistoryDetailsMerged = new HashMap<>();

    static {
        oncHistoryDetailsMerged.putAll(oncHistoryDetails1);
        oncHistoryDetailsMerged.putAll(oncHistoryDetails2);
        oncHistoryDetailsMerged.putAll(oncHistoryDetails3);
        oncHistoryDetailsMerged.putAll(oncHistoryDetails4);
    }
}
