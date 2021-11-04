package org.broadinstitute.dsm.model.elastic.migrationscript;

import java.util.HashMap;
import java.util.Map;

import static org.broadinstitute.dsm.model.elastic.export.parse.TypeParser.*;
import static org.broadinstitute.dsm.model.elastic.export.parse.TypeParser.TEXT_KEYWORD_MAPPING;

public class OncHistoryDetailsMigrate {

    private static final Map<String, Object> oncHistoryDetails1 = Map.of (
            "ddpParticipantId", TEXT_KEYWORD_MAPPING,
            "oncHistoryDetailId", TEXT_KEYWORD_MAPPING,
            "request", TEXT_KEYWORD_MAPPING,
            "deleted", BOOLEAN_MAPPING,
            "faxSent", DATE_MAPPING, //DATE?
            "tissueReceived", TEXT_KEYWORD_MAPPING,
            "medicalRecordId", TEXT_KEYWORD_MAPPING,
            "datePx", DATE_MAPPING, //DATE?
            "typePx", TEXT_KEYWORD_MAPPING,
            "locationPx", TEXT_KEYWORD_MAPPING );

    private static final Map<String, Object> oncHistoryDetails2 = Map.of (
            "histology", TEXT_KEYWORD_MAPPING,
            "accessionNumber", TEXT_KEYWORD_MAPPING,
            "facility", TEXT_KEYWORD_MAPPING, //DATE?
            "phone", TEXT_KEYWORD_MAPPING,
            "fax", TEXT_KEYWORD_MAPPING, //DATE?
            "notes", TEXT_KEYWORD_MAPPING, //DATE?
            "additionalValuesJson", TEXT_KEYWORD_MAPPING,
            "faxSentBy", TEXT_KEYWORD_MAPPING, //DATE?
            "faxConfirmed", DATE_MAPPING, //DATE?
            "faxSent2", DATE_MAPPING ); //DATE?

    private static final Map<String, Object> oncHistoryDetails3 = Map.of (
            "faxSent2By", TEXT_KEYWORD_MAPPING, //DATE?
            "faxConfirmed2", DATE_MAPPING, //DATE?
            "faxSent3", DATE_MAPPING,
            "faxSent3By", TEXT_KEYWORD_MAPPING,
            "faxConfirmed3", DATE_MAPPING,
            "tissueReceived", TEXT_KEYWORD_MAPPING,
            "tissueProblemOption", TEXT_KEYWORD_MAPPING, //HERE
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
