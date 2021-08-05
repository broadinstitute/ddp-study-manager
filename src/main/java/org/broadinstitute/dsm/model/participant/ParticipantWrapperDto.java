package org.broadinstitute.dsm.model.participant;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;
import org.broadinstitute.dsm.db.AbstractionActivity;
import org.broadinstitute.dsm.db.AbstractionGroup;
import org.broadinstitute.dsm.db.KitRequestShipping;
import org.broadinstitute.dsm.db.MedicalRecord;
import org.broadinstitute.dsm.db.OncHistoryDetail;
import org.broadinstitute.dsm.db.Participant;
import org.broadinstitute.dsm.db.ParticipantData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Getter
@Setter
public class ParticipantWrapperDto {

    private static final Logger logger = LoggerFactory.getLogger(ParticipantWrapperDto.class);

    private Map<String, Object> data;
    private Participant participant;
    private List<MedicalRecord> medicalRecords;
    private List<OncHistoryDetail> oncHistoryDetails;
    private List<KitRequestShipping> kits;
    private List<AbstractionActivity> abstractionActivities;
    private List<AbstractionGroup> abstractionSummary;
    private List<Map<String, Object>> proxyData;
    private List<ParticipantData> participantData;

    public ParticipantWrapperDto(Map<String, Object> data, Participant participant, List<MedicalRecord> medicalRecords,
                              List<OncHistoryDetail> oncHistoryDetails, List<KitRequestShipping> kits, List<AbstractionActivity> abstractionActivities,
                              List<AbstractionGroup> abstractionSummary, List<Map<String, Object>> proxyData, List<ParticipantData> participantData) {
        this.data = data;
        this.participant = participant;
        this.medicalRecords = medicalRecords;
        this.oncHistoryDetails = oncHistoryDetails;
        this.kits = kits;
        this.abstractionActivities = abstractionActivities;
        this.abstractionSummary = abstractionSummary;
        this.proxyData = proxyData;
        this.participantData = participantData;
    }

    public ParticipantWrapperDto() {

    }

    public static List<ParticipantWrapperDto> addAllData(List<String> baseList, Map<String, Map<String, Object>> esDataMap,
                                                      Map<String, Participant> participantMap, Map<String, List<MedicalRecord>> medicalRecordMap,
                                                      Map<String, List<OncHistoryDetail>> oncHistoryMap, Map<String, List<KitRequestShipping>> kitRequestMap,
                                                      Map<String, List<AbstractionActivity>> abstractionActivityMap, Map<String, List<AbstractionGroup>> abstractionSummary,
                                                      Map<String, Map<String, Object>> proxyData, Map<String, List<ParticipantData>> participantData) {
        List<ParticipantWrapperDto> participantList = new ArrayList<>();
        for (String ddpParticipantId : baseList) {
            Participant participant = participantMap != null ? participantMap.get(ddpParticipantId) : null;
            Map<String, Object> participantESData = esDataMap.get(ddpParticipantId);
            if (participantESData != null) {
                participantList.add(new ParticipantWrapperDto(participantESData, participant,
                        medicalRecordMap != null ? medicalRecordMap.get(ddpParticipantId) : null,
                        oncHistoryMap != null ? oncHistoryMap.get(ddpParticipantId) : null,
                        kitRequestMap != null ? kitRequestMap.get(ddpParticipantId) : null,
                        abstractionActivityMap != null ? abstractionActivityMap.get(ddpParticipantId) : null,
                        abstractionSummary != null ? abstractionSummary.get(ddpParticipantId) : null,
                        ParticipantWrapper.getProxyProfiles(participantESData, proxyData),
                participantData != null ? participantData.get(ddpParticipantId) : null));
            }
        }
        logger.info("Returning list w/ " + participantList.size() + " pts now");
        return participantList;
    }
}
