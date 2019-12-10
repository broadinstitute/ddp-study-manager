package org.broadinstitute.dsm.util;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.ddp.handlers.util.KitDetail;
import org.broadinstitute.dsm.DSMServer;
import org.broadinstitute.dsm.db.DDPInstance;
import org.broadinstitute.dsm.db.KitRequestShipping;
import org.broadinstitute.dsm.db.LatestKitRequest;
import org.broadinstitute.dsm.model.KitRequest;
import org.broadinstitute.dsm.model.KitRequestSettings;
import org.broadinstitute.dsm.model.KitSubKits;
import org.broadinstitute.dsm.model.KitType;
import org.broadinstitute.dsm.model.ddp.DDPParticipant;
import org.broadinstitute.dsm.statics.DBConstants;
import org.broadinstitute.dsm.statics.RoutePath;
import org.broadinstitute.dsm.util.externalShipper.ExternalShipper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class DDPKitRequest {

    private static final Logger logger = LoggerFactory.getLogger(DDPKitRequest.class);

    /**
     * Requesting 'new' DDPKitRequests and write them into ddp_kit_request
     *
     * @param latestKitRequests List<LatestKitRequest>
     * @throws Exception
     */
    public void requestAndWriteKitRequests(List<LatestKitRequest> latestKitRequests) {
        logger.info("Request kits from the DDPs");
        try {
            for (LatestKitRequest latestKit : latestKitRequests) {
                try {
                    String dsmRequest = latestKit.getBaseURL() + RoutePath.DDP_KIT_REQUEST;
                    if (latestKit.getLatestDDPKitRequestID() != null) {
                        dsmRequest += "/" + latestKit.getLatestDDPKitRequestID();
                    }
                    KitDetail[] kitDetails = DDPRequestUtil.getResponseObject(KitDetail[].class, dsmRequest, latestKit.getInstanceName(), latestKit.isHasAuth0Token());
                    if (kitDetails != null) {
                        logger.info("Got " + kitDetails.length + " 'new' KitRequests from " + latestKit.getInstanceName());
                        Map<String, Map<String, Object>> participantsESData = null;
                        if (kitDetails.length > 0) {

                            if (StringUtils.isNotBlank(latestKit.getParticipantIndexES())) {
                                //could be filtered as well to have a smaller list
                                participantsESData = ElasticSearchUtil.getDDPParticipantsFromES(latestKit.getInstanceName(), latestKit.getParticipantIndexES());
                            }

                            Map<String, KitType> kitTypes = KitType.getKitLookup();
                            Map<Integer, KitRequestSettings> kitRequestSettingsMap = KitRequestSettings.getKitRequestSettings(latestKit.getInstanceID());

                            Map<KitRequestSettings, ArrayList<KitRequest>> kitsToOrder = new HashMap<>();

                            for (KitDetail kitDetail : kitDetails) {
                                if (kitDetail != null && kitDetail.getParticipantId() != null && kitDetail.getKitRequestId() != null
                                        && kitDetail.getKitType() != null) {
                                    String key = kitDetail.getKitType() + "_" + latestKit.getInstanceID();
                                    KitType kitType = kitTypes.get(key);
                                    if (kitType != null) {
                                        KitRequestSettings kitRequestSettings = kitRequestSettingsMap.get(kitType.getKitTypeId());

                                        if (StringUtils.isNotBlank(latestKit.getParticipantIndexES())) {
                                            //without order list, that was only added for promise and currently is not used!
                                            if (participantsESData != null && !participantsESData.isEmpty()) {
                                                Map<String, Object> participantESData = participantsESData.get(key);
                                                if (participantESData != null && !participantESData.isEmpty()) {
                                                    Map<String, Object> profile = (Map<String, Object>) participantESData.get("profile");
                                                    if (profile != null && !profile.isEmpty()) {
                                                        String collaboratorParticipantId = KitRequestShipping.getCollaboratorParticipantId(latestKit.getBaseURL(), latestKit.getInstanceID(), latestKit.isMigrated(),
                                                                latestKit.getCollaboratorIdPrefix(), (String) profile.get("guid"), (String) profile.get("hruid"), kitRequestSettings.getCollaboratorParticipantLengthOverwrite());

                                                        KitRequestShipping.addKitRequests(latestKit.getInstanceID(), kitDetail, kitType.getKitTypeId(),
                                                                kitRequestSettings, collaboratorParticipantId);
                                                    }
                                                }
                                            }
                                        }
                                        else {
                                            DDPParticipant participant = DDPParticipant.getDDPParticipant(latestKit.getBaseURL(), latestKit.getInstanceName(), kitDetail.getParticipantId(), latestKit.isHasAuth0Token());
                                            if (participant != null) {
                                                // if the kit type has sub kits > like for promise
                                                boolean kitHasSubKits = kitRequestSettings.getHasSubKits() != 0;

                                                ArrayList<KitRequest> orderKit = kitsToOrder.get(kitRequestSettings);
                                                if (orderKit == null) {
                                                    orderKit = new ArrayList<>();
                                                }

                                                String collaboratorParticipantId = KitRequestShipping.getCollaboratorParticipantId(latestKit.getBaseURL(), latestKit.getInstanceID(), latestKit.isMigrated(),
                                                        latestKit.getCollaboratorIdPrefix(), participant.getParticipantId(), participant.getShortId(), kitRequestSettings.getCollaboratorParticipantLengthOverwrite());
                                                //only promise for now
                                                if (kitHasSubKits) {
                                                    List<KitSubKits> subKits = kitRequestSettings.getSubKits();
                                                    for (KitSubKits subKit : subKits) {
                                                        for (int i = 0; i < subKit.getKitCount(); i++) {
                                                            KitRequestShipping.addKitRequests(latestKit.getInstanceID(), kitDetail, subKit.getKitTypeId(),
                                                                    kitRequestSettings, collaboratorParticipantId);
                                                        }
                                                    }
                                                    if (StringUtils.isNotBlank(kitRequestSettings.getExternalShipper())) {
                                                        orderKit.add(new KitRequest(kitDetail.getParticipantId(), participant.getShortId(), participant));
                                                    }
                                                }
                                                else {
                                                    // all other ddps
                                                    KitRequestShipping.addKitRequests(latestKit.getInstanceID(), kitDetail, kitType.getKitTypeId(),
                                                            kitRequestSettings, collaboratorParticipantId);
                                                    if (StringUtils.isNotBlank(kitRequestSettings.getExternalShipper())) {
                                                        orderKit.add(new KitRequest(kitDetail.getParticipantId(), participant.getShortId(), participant));
                                                    }
                                                }
                                                if (StringUtils.isNotBlank(kitRequestSettings.getExternalShipper())) {
                                                    kitsToOrder.put(kitRequestSettings, orderKit);
                                                }
                                            }
                                            else {
                                                throw new RuntimeException("No participant returned w/ " + kitDetail.getParticipantId() + " for " + latestKit.getInstanceName());
                                            }
                                        }
                                    }
                                    else {
                                        throw new RuntimeException("KitTypeId is not in kit_type table. KitTypeId " + kitDetail.getKitType());
                                    }
                                }
                                else {
                                    logger.error("Important information for DDPKitRequest is missing. " +
                                                         kitDetail == null ? " DDPKitRequest is null " : " participantId " + kitDetail.getParticipantId() +
                                                         " kitRequest.getKitRequestId() " + kitDetail.getKitRequestId() +
                                                         " kitRequest.getKitType() " + kitDetail.getKitType());
                                    throw new RuntimeException("Important information for kitRequest is missing");
                                }
                            }

                            //only order if kit were added to kits to order hash (which should only be if a kit has an external shipper)
                            if (!kitsToOrder.isEmpty()) {
                                Iterator<KitRequestSettings> iter = kitsToOrder.keySet().iterator();
                                while (iter.hasNext()) {
                                    KitRequestSettings setting = iter.next();
                                    ArrayList<KitRequest> kits = kitsToOrder.get(setting);
                                    try {
                                        ExternalShipper shipper = (ExternalShipper) Class.forName(DSMServer.getClassName(setting.getExternalShipper())).newInstance();
                                        shipper.orderKitRequests(kits, new EasyPostUtil(latestKit.getInstanceName()), setting);
                                    }
                                    catch (Exception e) {
                                        logger.error("Failed to sent kit request order to " + setting.getExternalShipper(), e);
                                    }
                                }
                            }
                        }
                    }
                    else {
                        logger.info("Didn't receive any kit requests from " + latestKit.getInstanceName());
                    }
                }
                catch (Exception ex) {
                    logger.error("Error requesting KitRequests form " + latestKit.getInstanceName(), ex);
                }
            }
        }
        catch (Exception e) {
            throw new RuntimeException("Error getting KitRequests ", e);
        }
    }
}
