package org.broadinstitute.dsm.model;

import lombok.Data;
import org.broadinstitute.dsm.db.dto.kit.ClinicalKitDto;
@Data
public class ClinicalKitWrapper {
    ClinicalKitDto clinicalKitDto;
    String ddpParticipantId;
    Integer ddpInstanceId;

    public ClinicalKitWrapper(ClinicalKitDto clinicalKitDto, int ddpInstanceId, String ddpParticipantId) {
        this.clinicalKitDto = clinicalKitDto;
        this.ddpInstanceId = ddpInstanceId;
        this.ddpParticipantId = ddpParticipantId;
    }
}
