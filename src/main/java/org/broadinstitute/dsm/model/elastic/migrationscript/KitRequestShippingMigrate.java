package org.broadinstitute.dsm.model.elastic.migrationscript;

import static org.broadinstitute.dsm.model.elastic.export.parse.TypeParser.BOOLEAN_MAPPING;
import static org.broadinstitute.dsm.model.elastic.export.parse.TypeParser.DATE_MAPPING;
import static org.broadinstitute.dsm.model.elastic.export.parse.TypeParser.TEXT_KEYWORD_MAPPING;

import java.util.Map;

public class KitRequestShippingMigrate {

    private static final Map<String, Object> kitRequestShippingMigrate1 = Map.of (
            "ddpParticipantId", TEXT_KEYWORD_MAPPING,
            "ddpInstanceId", TEXT_KEYWORD_MAPPING,
            "institutionId", TEXT_KEYWORD_MAPPING,
            "ddpInstitutionId", TEXT_KEYWORD_MAPPING,
            "type", TEXT_KEYWORD_MAPPING,
            "participantId", TEXT_KEYWORD_MAPPING,
            "medicalRecordId", TEXT_KEYWORD_MAPPING,
            "name", TEXT_KEYWORD_MAPPING,
            "contact", TEXT_KEYWORD_MAPPING );

    private static final Map<String, Object> kitRequestShippingMigrate2 = Map.of (
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

    private static final Map<String, Object> medicalRecordMapping3 = Map.of (
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

    private static final Map<String, Object> medicalRecordMapping4 = Map.of (
            "followUpRequired", BOOLEAN_MAPPING,
            "followUpRequiredText", TEXT_KEYWORD_MAPPING,
            "international", BOOLEAN_MAPPING,
            "crRequired", BOOLEAN_MAPPING,
            "pathologyPresent", TEXT_KEYWORD_MAPPING,
            "notes", TEXT_KEYWORD_MAPPING,
            "additionalValuesJson", TEXT_KEYWORD_MAPPING,
            "reviewMedicalRecord", TEXT_KEYWORD_MAPPING
    );


}
