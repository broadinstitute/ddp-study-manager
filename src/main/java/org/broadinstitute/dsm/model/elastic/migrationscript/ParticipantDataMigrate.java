package org.broadinstitute.dsm.model.elastic.migrationscript;

import static org.broadinstitute.dsm.model.elastic.export.parse.TypeParser.BOOLEAN_MAPPING;
import static org.broadinstitute.dsm.model.elastic.export.parse.TypeParser.TEXT_KEYWORD_MAPPING;

import java.util.Map;

public class ParticipantDataMigrate {


    private static final Map<String, Object> participantDataMigrate1 = Map.of (
            "participantDataId", TEXT_KEYWORD_MAPPING,
            "ddpParticipantId", TEXT_KEYWORD_MAPPING,
            "ddpInstanceId", TEXT_KEYWORD_MAPPING,
            "fieldTypeId", TEXT_KEYWORD_MAPPING
    );




}
