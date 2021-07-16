package org.broadinstitute.dsm.model.familymember;

import static org.junit.Assert.*;

import java.util.HashMap;

import org.broadinstitute.ddp.handlers.util.Result;
import org.broadinstitute.dsm.db.dto.ddp.participant.ParticipantDataDto;
import org.broadinstitute.dsm.model.participant.data.AddFamilyMemberPayload;
import org.broadinstitute.dsm.model.participant.data.ParticipantData;
import org.broadinstitute.dsm.route.familymember.AddFamilyMemberRouteTest;
import org.junit.Assert;
import org.junit.Test;

public class AddFamilyMemberTest {


    public static class TestAddFamilyMember extends AddFamilyMember {

        public TestAddFamilyMember(AddFamilyMemberPayload addFamilyMemberPayload) {
            super(addFamilyMemberPayload);
        }

        @Override
        public void exportDataToEs() {

        }
    }
}