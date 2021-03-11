package org.broadinstitute.dsm.model.ddp;


import java.util.Map;
import java.util.Optional;

import com.google.gson.annotations.SerializedName;

public class AddFamilyMemberPayload {

    private String participantGuid;
    private String realm;
    private FamilyMemberDetails data;
    private Integer userId;

    public AddFamilyMemberPayload(String participantGuid, String realm, FamilyMemberDetails data, Integer userId) {
        this.participantGuid = participantGuid;
        this.realm = realm;
        this.data = data;
        this.userId = userId;
    }

    public Optional<String> getParticipantGuid() {
        return Optional.ofNullable(participantGuid);
    }

    public Optional<String> getRealm() {
        return Optional.ofNullable(realm);
    }

    public Optional<FamilyMemberDetails> getData() {
        return Optional.ofNullable(data);
    }

    public Optional<Integer> getUserId() {
        return Optional.ofNullable(userId);
    }
}
