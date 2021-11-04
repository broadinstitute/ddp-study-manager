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
            "facility", TEXT_KEYWORD_MAPPING,
            "phone", TEXT_KEYWORD_MAPPING,
            "fax", TEXT_KEYWORD_MAPPING,
            "notes", TEXT_KEYWORD_MAPPING,
            "additionalValuesJson", TEXT_KEYWORD_MAPPING,
            "faxSentBy", TEXT_KEYWORD_MAPPING,
            "faxConfirmed", DATE_MAPPING, //DATE?
            "faxSent2", DATE_MAPPING ); //DATE?

    private static final Map<String, Object> oncHistoryDetails3 = Map.of (
            "faxSent2By", TEXT_KEYWORD_MAPPING,
            "faxConfirmed2", DATE_MAPPING, //DATE?
            "faxSent3", DATE_MAPPING,
            "faxSent3By", TEXT_KEYWORD_MAPPING,
            "faxConfirmed3", DATE_MAPPING,
            "tissueReceived", TEXT_KEYWORD_MAPPING,
            "tissueProblemOption", TEXT_KEYWORD_MAPPING,
            "gender", TEXT_KEYWORD_MAPPING,
            "destructionPolicy", TEXT_KEYWORD_MAPPING,
            "unableObtainTissue", BOOLEAN_MAPPING);

    private static final Map<String, Object> oncHistoryDetails4 = Map.of (
            "tissueId", TEXT_KEYWORD_MAPPING,
            "countReceived", TEXT_KEYWORD_MAPPING,
            "tissueType", TEXT_KEYWORD_MAPPING,
            "tissueSite", TEXT_KEYWORD_MAPPING,
            "hE", TEXT_KEYWORD_MAPPING,
            "pathologyReport", TEXT_KEYWORD_MAPPING,
            "collaboratorSampleId", TEXT_KEYWORD_MAPPING,
            "blockSent", DATE_MAPPING, // DATE?
            "scrollsReceived", DATE_MAPPING, // DATE?
            "skId", TEXT_KEYWORD_MAPPING
    );

    private static final Map<String, Object> oncHistoryDetails5 = Map.of (
            "smId", TEXT_KEYWORD_MAPPING,
            "sentGp", DATE_MAPPING, // DATE?,
            "firstSmId", TEXT_KEYWORD_MAPPING,
            "additionalTissueValueJson", TEXT_KEYWORD_MAPPING,
            "expectedReturn", DATE_MAPPING, // DATE?????????????
            "returnDate", DATE_MAPPING, // DATE?
            "returnFedexId", TEXT_KEYWORD_MAPPING,
            "shlWorkNumber", TEXT_KEYWORD_MAPPING,
            "tumorPercentage", TEXT_KEYWORD_MAPPING,
            "tissueSequence", TEXT_KEYWORD_MAPPING
    );

    private static final Map<String, Object> oncHistoryDetails6 = Map.of (
            "scrollsCount", TEXT_KEYWORD_MAPPING,
            "ussCount", TEXT_KEYWORD_MAPPING,
            "hECount", TEXT_KEYWORD_MAPPING,
            "blocksCount", TEXT_KEYWORD_MAPPING
    );

    private static final Map<String, Object> oncHistoryDetailsMerged = MapAdapter.of(
            oncHistoryDetails1,
            oncHistoryDetails2,
            oncHistoryDetails3,
            oncHistoryDetails4,
            oncHistoryDetails5,
            oncHistoryDetails6
            );

}
