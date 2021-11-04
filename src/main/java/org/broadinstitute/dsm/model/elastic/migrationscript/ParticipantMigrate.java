package org.broadinstitute.dsm.model.elastic.migrationscript;

import static org.broadinstitute.dsm.model.elastic.export.parse.TypeParser.BOOLEAN_MAPPING;
import static org.broadinstitute.dsm.model.elastic.export.parse.TypeParser.DATE_MAPPING;
import static org.broadinstitute.dsm.model.elastic.export.parse.TypeParser.TEXT_KEYWORD_MAPPING;

import java.util.Map;

public class ParticipantMigrate {


    private static final Map<String, Object> participantMigrate1 = Map.of (
            "participantId", TEXT_KEYWORD_MAPPING,
            "ddpParticipantId", TEXT_KEYWORD_MAPPING,
            "assigneeIdMr", TEXT_KEYWORD_MAPPING,
            "assigneeIdTissue", TEXT_KEYWORD_MAPPING,
            "ddpInstanceId", TEXT_KEYWORD_MAPPING,
            "instanceName", TEXT_KEYWORD_MAPPING,
            "baseUrl", TEXT_KEYWORD_MAPPING,
            "mrAttentionFlagD", TEXT_KEYWORD_MAPPING,
            "tissueAttentionFlagD", TEXT_KEYWORD_MAPPING,
            "auth0Token", BOOLEAN_MAPPING
    );

    private static final Map<String, Object> participantMigrate2 = Map.of (
            "notificationRecipients", TEXT_KEYWORD_MAPPING,
            "migratedDdp", BOOLEAN_MAPPING,
            "oncHistoryId", TEXT_KEYWORD_MAPPING,
            "created", DATE_MAPPING,
            "reviewed", DATE_MAPPING,
            "crSent", DATE_MAPPING, //DATE?
            "crReceived", DATE_MAPPING, //DATE?
            "notes", TEXT_KEYWORD_MAPPING,
            "minimalMr", TEXT_KEYWORD_MAPPING,
            "abstractionReady", TEXT_KEYWORD_MAPPING
    );

    private static final Map<String, Object> participantMigrate3 = Map.of (
            "additionalValuesJson", TEXT_KEYWORD_MAPPING,
            "exitDate", TEXT_KEYWORD_MAPPING,
            "exitBy", TEXT_KEYWORD_MAPPING
    );

    private static final Map<String, Object> participantMerged = MapAdapter.of(
            participantMigrate1,
            participantMigrate2,
            participantMigrate3
    );


}
