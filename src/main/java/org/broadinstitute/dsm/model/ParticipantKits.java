package org.broadinstitute.dsm.model;

import org.broadinstitute.dsm.db.KitRequestShipping;

import java.util.List;

public class ParticipantKits {
    String particpantId;
    List<KitRequestShipping> samples;

    public ParticipantKits(String particpantId, List<KitRequestShipping> samples) {
        this.particpantId = particpantId;
        this.samples = samples;
    }
}
