package org.broadinstitute.dsm.model.elastic.migrationscript;

import static org.broadinstitute.dsm.model.elastic.export.parse.TypeParser.BOOLEAN_MAPPING;
import static org.broadinstitute.dsm.model.elastic.export.parse.TypeParser.DATE_MAPPING;
import static org.broadinstitute.dsm.model.elastic.export.parse.TypeParser.TEXT_KEYWORD_MAPPING;

import java.util.Map;

public class KitRequestShippingMigrate {

    private static final Map<String, Object> kitRequestShippingMigrate1 = Map.of (
            "kitTypeName", TEXT_KEYWORD_MAPPING,
            "instanceName", TEXT_KEYWORD_MAPPING,
            "bspCollaboratorParticipantId", TEXT_KEYWORD_MAPPING,
            "bspCollaboratorSampleId", TEXT_KEYWORD_MAPPING,
            "ddpParticipantId", TEXT_KEYWORD_MAPPING,
            "ddpLabel", TEXT_KEYWORD_MAPPING,
            "dsmKitRequest", TEXT_KEYWORD_MAPPING,
            "kitTypeId", TEXT_KEYWORD_MAPPING,
            "externalOrderStatus", TEXT_KEYWORD_MAPPING,
            "externalOrderNumber", TEXT_KEYWORD_MAPPING
            );

    private static final Map<String, Object> kitRequestShippingMigrate2 = Map.of (
            "externalOrderDate", TEXT_KEYWORD_MAPPING,
            "externalResponse", TEXT_KEYWORD_MAPPING,
            "uploadReason", TEXT_KEYWORD_MAPPING,
            "noReturn", BOOLEAN_MAPPING,
            "createdBy", TEXT_KEYWORD_MAPPING,
            "dsmKitRequestId", TEXT_KEYWORD_MAPPING,
            "dsmKitId", TEXT_KEYWORD_MAPPING,
            "kitComplete", BOOLEAN_MAPPING,
            "labelUrlTo", TEXT_KEYWORD_MAPPING,
            "labelUrlReturn", TEXT_KEYWORD_MAPPING
    );

    private static final Map<String, Object> kitRequestShippingMigrate3 = Map.of (
            "trackingToId", TEXT_KEYWORD_MAPPING,
            "trackingReturnId", TEXT_KEYWORD_MAPPING,
            "easypostTrackingToUrl", TEXT_KEYWORD_MAPPING,
            "easypostTrackingReturnUrl", TEXT_KEYWORD_MAPPING,
            "easypostToId", TEXT_KEYWORD_MAPPING,
            "easypostShipmentStatus", BOOLEAN_MAPPING,
            "scanDate", TEXT_KEYWORD_MAPPING,
            "labelDate", TEXT_KEYWORD_MAPPING,
            "error", BOOLEAN_MAPPING,
            "message", TEXT_KEYWORD_MAPPING);

    private static final Map<String, Object> kitRequestShippingMigrate4 = Map.of (
            "receiveDate", TEXT_KEYWORD_MAPPING,
            "deactivatedDate", TEXT_KEYWORD_MAPPING,
            "easypostAddressIdTo", TEXT_KEYWORD_MAPPING,
            "deactivationReason", TEXT_KEYWORD_MAPPING,
            "trackingId", TEXT_KEYWORD_MAPPING,
            "kitLabel", TEXT_KEYWORD_MAPPING,
            "express", BOOLEAN_MAPPING,
            "testResult", TEXT_KEYWORD_MAPPING,
            "needsApproval", BOOLEAN_MAPPING,
            "authorization", TEXT_KEYWORD_MAPPING
    );

    private static final Map<String, Object> kitRequestShippingMigrate5 = Map.of (
            "denialReason", TEXT_KEYWORD_MAPPING,
            "authorizedBy", TEXT_KEYWORD_MAPPING,
            "upsTrackingStatus", TEXT_KEYWORD_MAPPING,
            "upsReturnStatus", TEXT_KEYWORD_MAPPING,
            "CEOrder", BOOLEAN_MAPPING // ? what is this ?
    );

    private static final Map<String, Object> KitRequestShippingMerged = MapAdapter.of(
            kitRequestShippingMigrate1,
            kitRequestShippingMigrate2,
            kitRequestShippingMigrate3,
            kitRequestShippingMigrate4,
            kitRequestShippingMigrate5
    );


}
